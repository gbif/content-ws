package org.gbif.content.resource;

import org.gbif.content.conf.ContentWsConfiguration;
import org.gbif.content.utils.ConversionUtil;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import com.codahale.metrics.annotation.Timed;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



@Path("newsroom/")
@Produces(MediaType.APPLICATION_JSON)
public class EventsResource {

  private static final Logger LOG = LoggerFactory.getLogger(EventsResource.class);

  private static final String START_FIELD = "start.en-GB";

  private static final String CREATED_AT_FIELD = "createdAt";

  private static final Optional<QueryBuilder> UPCOMING_EVENTS = Optional.of(QueryBuilders.rangeQuery(START_FIELD)
                                                                   .gte("now/d").includeUpper(Boolean.FALSE));

  private static final String MEDIA_TYPE_ICAL = "text/iCal";

  private final Client esClient;

  private final ContentWsConfiguration configuration;

  public EventsResource(Client esClient, ContentWsConfiguration configuration) {
    this.esClient = esClient;
    this.configuration = configuration;
  }

  @GET
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MEDIA_TYPE_ICAL)
  @Path("events/calendar/upcoming.ics")
  public String getUpcomingEventsICal(@QueryParam("query") String query) {
    ICalendar iCal = new ICalendar();
    executeQuery(query, UPCOMING_EVENTS, START_FIELD, configuration.getEsEventsIndex())
      .getHits().forEach(searchHit -> iCal.addEvent(ConversionUtil.toVEvent(searchHit)));
    return Biweekly.write(iCal).go();
  }

  @GET
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_ATOM_XML)
  @Path("events/upcoming.xml")
  public String getUpComingEvents(@QueryParam("query") String query) {
    return toXmlAtomFeed(newEventsFeed(), query, UPCOMING_EVENTS, START_FIELD, configuration.getEsEventsIndex());
  }

  @GET
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_ATOM_XML)
  @Path("news/rss/{region}")
  public String getNewsByRegion(@QueryParam("query") String query, @PathParam("region") String region) {
    return toXmlAtomFeed(newEventsFeed(), query, Optional.of(QueryBuilders.termQuery("region", region)),
                         CREATED_AT_FIELD, configuration.getEsNewsIndex());
  }

  @GET
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_ATOM_XML)
  @Path("news/rss/bid/{language}")
  public String getBidNews(@QueryParam("query") String query, @PathParam("language") String language) {
    return toXmlAtomFeed(newEventsFeed(), query, Optional.of(QueryBuilders.termQuery("language", language)),
                         CREATED_AT_FIELD, configuration.getEsNewsIndex());
  }

  @GET
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  @Path("news/json/bid/{language}")
  public List<SyndEntry> getBidNewsJson(@QueryParam("query") String query, @PathParam("language") String language) {
    return Arrays.stream(executeQuery(query, Optional.of(QueryBuilders.termQuery("language", language)),
                                      CREATED_AT_FIELD, configuration.getEsNewsIndex()).getHits().hits())
            .map(ConversionUtil::toFeedEntry).collect(Collectors.toList());
  }

  @GET
  @Timed
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_ATOM_XML)
  @Path("news/uses/rss")
  public String getDataUses(@QueryParam("query") String query) {
    return toXmlAtomFeed(newEventsFeed(), query, Optional.empty(), CREATED_AT_FIELD, configuration.getEsDataUseIndex());
  }

  public String toXmlAtomFeed(SyndFeed feed, String query, Optional<QueryBuilder> filter, String dateSortField, String idxName) {
    try {
      feed.setEntries(Arrays.stream(executeQuery(query, filter, dateSortField, idxName).getHits().hits())
                        .map(ConversionUtil::toFeedEntry).collect(Collectors.toList()));
      StringWriter writer = new StringWriter();
      new SyndFeedOutput().output(feed, writer);
      return writer.toString();
    } catch (IOException | FeedException ex) {
      LOG.error("Error generating events RSS feed", ex);
      throw new WebApplicationException();
    }
  }

  /**
   * Executes the default search query.
   */
  private SearchResponse executeQuery(String query, Optional<QueryBuilder> filter, String dateSortField, String idxName) {
    return esClient.prepareSearch(idxName)
      .setQuery(QueryBuilders.boolQuery()
                  .filter(filter.get())
                  .must(query == null ? QueryBuilders.matchAllQuery() : QueryBuilders.wrapperQuery(query)))
      .addSort(dateSortField, SortOrder.DESC)
      .execute()
      .actionGet();
  }

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


}
