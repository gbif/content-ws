package org.gbif.content.resource;

import org.gbif.content.conf.ContentWsConfiguration;
import org.gbif.content.utils.ConversionUtil;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import biweekly.Biweekly;
import biweekly.ICalendar;
import ch.qos.logback.core.status.Status;
import com.codahale.metrics.annotation.Timed;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.util.LocaleUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource class that provides RSS and iCal feeds for events and news.
 */
@Path("newsroom/")
@Produces(MediaType.APPLICATION_JSON)
public class EventsResource {

  private static final Logger LOG = LoggerFactory.getLogger(EventsResource.class);

  private static final String START_FIELD = "start";

  private static final String CREATED_AT_FIELD = "createdAt";

  private static final String GBIF_REGION_FIELD = "gbifRegion";

  private static final Pattern HYPHEN = Pattern.compile("-");

  /**
   * Default page size for ElasticSearch
   */
  private static final int DEFAULT_SIZE = 10;

  private static final Optional<QueryBuilder> UPCOMING_EVENTS = Optional.of(QueryBuilders.rangeQuery(START_FIELD)
                                                                   .gte("now/d").includeUpper(Boolean.FALSE));

  private static final QueryBuilder SEARCHABLE = QueryBuilders.termQuery("searchable", Boolean.TRUE);

  private static final String MEDIA_TYPE_ICAL = "text/iCal";

  private final Client esClient;

  private final ContentWsConfiguration configuration;

  /**
   * Creates a new Rss Feed using the common GBIF content.
   */
  private static SyndFeed newEventsFeed() {
    SyndFeed feed = new SyndFeedImpl();
    feed.setFeedType("rss_2.0");
    feed.setTitle("Upcoming events");
    feed.setDescription("GBIF Upcoming News");
    feed.setLanguage("en");
    feed.setLink("http://www.gbif.org/newsroom/events/upcoming.xml");
    return feed;
  }

  /**
   * Creates a new Rss Feed using the common GBIF content.
   */
  private static SyndFeed newNewsFeed() {
    SyndFeed feed = new SyndFeedImpl();
    feed.setFeedType("rss_2.0");
    feed.setTitle("GBIF news feed");
    feed.setDescription("GBIF News");
    feed.setLanguage("en");
    feed.setLink("http://www.gbif.org/newsroom/news/rss");
    return feed;
  }

  /**
   * Full constructor.
   * @param esClient ElasticSearch client
   * @param configuration  configuration settings
   */
  public EventsResource(Client esClient, ContentWsConfiguration configuration) {
    this.esClient = esClient;
    this.configuration = configuration;
  }

  /**
   * Upcoming events in iCal format.
   */
  @GET
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MEDIA_TYPE_ICAL)
  @Path("events/calendar/upcoming.ics")
  public String getUpcomingEventsICal() {
    ICalendar iCal = new ICalendar();
    executeQuery(UPCOMING_EVENTS, START_FIELD, configuration.getEsEventsIndex())
      .getHits().forEach(searchHit -> iCal.addEvent(ConversionUtil.toVEvent(searchHit,
                                                                            configuration.getDefaultLocale())));
    return Biweekly.write(iCal).go();
  }

  /**
   * Upcoming events RSS feed.
   */
  @GET
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_ATOM_XML)
  @Path("events/upcoming.xml")
  public String getUpComingEvents() {
    return toXmlAtomFeed(newEventsFeed(), UPCOMING_EVENTS, START_FIELD, configuration.getEsEventsIndex());
  }

  /**
   * Single event RSS feed in Atom format.
   */
  @GET
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MEDIA_TYPE_ICAL)
  @Path("events/{eventId}")
  public String getEvent(@PathParam("eventId") String eventId) {
    ICalendar iCal = new ICalendar();
    iCal.addEvent(ConversionUtil
                    .toVEvent(esClient.prepareGet(configuration.getEsEventsIndex(), "content", eventId).get(),
                              configuration.getDefaultLocale()));
    return Biweekly.write(iCal).go();
  }

  /**
   * News RSS feeds.
   */
  @GET
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_ATOM_XML)
  @Path("news/rss")
  public String getNews() {
    return toXmlAtomFeed(newNewsFeed(), Optional.empty(), CREATED_AT_FIELD, configuration.getEsNewsIndex());
  }

  /**
   * New RSS feed for GBIF region.
   */
  @GET
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_ATOM_XML)
  @Path("news/rss/{gbifRegion}")
  public String getNewsByRegion(@PathParam("gbifRegion") String region) {
    return toXmlAtomFeed(newNewsFeed(), Optional.of(QueryBuilders.termQuery(GBIF_REGION_FIELD, region)),
                         CREATED_AT_FIELD, configuration.getEsNewsIndex());
  }

  /**
   * News RSS feed for a program and language.
   */
  @GET
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_ATOM_XML)
  @Path("news/rss/{acronym}/{language}")
  public String getProgramNews(@PathParam("acronym") String acronym, @PathParam("language") String language) {
    return toXmlAtomFeed(newNewsFeed(), Optional.of(programmeQuery(acronym)), CREATED_AT_FIELD,
                         configuration.getEsNewsIndex(), getLocale(language));
  }

  /**
   * JSON News for a program and language.
   */
  @GET
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("news/json/{acronym}/{language}")
  public List<SyndEntry> getProgrammeNewsJson(@PathParam("acronym") String acronym,
                                              @PathParam("language") String language) {
    return Arrays.stream(executeQuery(Optional.of(programmeQuery(acronym)),
                                      CREATED_AT_FIELD, configuration.getEsNewsIndex()).getHits().hits())
            .map(searchHit -> ConversionUtil.toFeedEntry(searchHit, getLocale(language)))
            .collect(Collectors.toList());
  }

  /**
   * Data uses RSS feed.
   */
  @GET
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_ATOM_XML)
  @Path("uses/rss")
  public String getDataUses() {
    return toXmlAtomFeed(newNewsFeed(), Optional.empty(), CREATED_AT_FIELD, configuration.getEsDataUseIndex());
  }

  /**
   * Parse language into a Locale.
   */
  private String getLocale(String language) {
    try {
      Optional<String> optLanguage = Optional.ofNullable(language);
      optLanguage.ifPresent(languageLocale -> LocaleUtils.parse(HYPHEN.matcher(language).replaceAll("_")));
      return optLanguage.orElse(configuration.getDefaultLocale());
    } catch (IllegalArgumentException ex) {
      throw new WebApplicationException(String.format("Language %s is not supported", language),
                                        Response.Status.BAD_REQUEST);
    }
  }

  /**
   * Builds a programme:id query builder.
   */
  private QueryBuilder programmeQuery(String acronym) {
    return QueryBuilders.termQuery("programmeTag", findProgrammeId(acronym));
  }

  /**
   * Finds the programme id by its acronym.
   */
  private String findProgrammeId(String acronym) {
    SearchResponse response = executeQuery(Optional.of(QueryBuilders.termQuery("acronym.keyword", acronym)),
                                                  CREATED_AT_FIELD, configuration.getEsProgrammeIndex());
    return Arrays.stream(response.getHits().getHits())
            .map(SearchHit::getId).findFirst()
            .orElseThrow(() -> new WebApplicationException(String.format("Project acronym %s not found", acronym),
                                                           Response.status(Response.Status.BAD_REQUEST).build()));
  }

  /**
   * Executes a query and translates the results into XML Atom Feeds.
   */
  private String toXmlAtomFeed(SyndFeed feed, Optional<QueryBuilder> filter, String dateSortField, String idxName) {
    return toXmlAtomFeed(feed, filter, dateSortField, idxName, configuration.defaultLocale);
  }

  /**
   * Executes a query and translates the results into XML Atom Feeds.
   */
  private String toXmlAtomFeed(SyndFeed feed, Optional<QueryBuilder> filter, String dateSortField, String idxName, String locale) {
    try {
      feed.setEntries(Arrays.stream(executeQuery(filter, dateSortField, idxName).getHits().hits())
                        .map(searchHit -> ConversionUtil.toFeedEntry(searchHit, locale))
                        .collect(Collectors.toList()));
      StringWriter writer = new StringWriter();
      new SyndFeedOutput().output(feed, writer);
      return writer.toString();
    } catch (IOException | FeedException ex) {
      LOG.error("Error generating events RSS feed", ex);
      throw new WebApplicationException(Status.ERROR);
    }
  }

  /**
   * Executes the default search query.
   */
  private SearchResponse executeQuery(String query, Optional<QueryBuilder> filter, String dateSortField,
                                      String idxName) {
    BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery()
                                             .filter(SEARCHABLE)
                                             .must(query == null ?
                                                     QueryBuilders.matchAllQuery() :
                                                     QueryBuilders.wrapperQuery(query));
    filter.ifPresent(queryBuilder::filter);
    return esClient.prepareSearch(idxName).setQuery(queryBuilder).addSort(dateSortField, SortOrder.DESC)
                    .setSize(DEFAULT_SIZE).execute().actionGet();
  }

  /**
   * Executes the default search query.
   */
  private SearchResponse executeQuery(Optional<QueryBuilder> filter, String dateSortField, String idxName) {
    return executeQuery(null, filter, dateSortField, idxName);
  }


}
