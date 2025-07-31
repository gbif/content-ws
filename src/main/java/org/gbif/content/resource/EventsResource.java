/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.content.resource;

import co.elastic.clients.elasticsearch.core.search.Hit;

import org.gbif.content.config.ContentWsProperties;
import org.gbif.content.exception.WebApplicationException;
import org.gbif.content.utils.ConversionUtil;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.Locale;
import java.util.Map;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetRequest;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch._types.query_dsl.DateRangeQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedOutput;

import biweekly.Biweekly;
import biweekly.ICalendar;

/**
 * Resource class that provides RSS and iCal feeds for events and news.
 */
@RequestMapping(value = "newsroom", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
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

  private static final int MAX_SIZE = 1_000;

  private static final Query UPCOMING_EVENTS =
    Query.of(q -> q.range(r -> r.date(DateRangeQuery.of(d -> d.field(START_FIELD).gte("now/d")))));

  private static final Query SEARCHABLE =
      Query.of(q -> q.term(t -> t.field("searchable").value(true)));

  private static final String MEDIA_TYPE_ICAL = "text/iCal";

  private final ElasticsearchClient esClient;

  private final ContentWsProperties configuration;

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
   *
   * @param esClient      ElasticSearch client
   * @param configuration configuration settings
   */
  public EventsResource(ElasticsearchClient esClient, ContentWsProperties configuration) {
    this.esClient = esClient;
    this.configuration = configuration;
  }

  /**
   * Upcoming events in iCal format.
   */
  @GetMapping(path = "events/calendar/upcoming.ics", produces = MEDIA_TYPE_ICAL)
  public String getUpcomingEventsICal(
      @RequestParam(value = "limit", required = false) Integer limit) {
    ICalendar iCal = new ICalendar();
    executeQuery(UPCOMING_EVENTS, START_FIELD, configuration.getEsEventsIndex(), limit)
        .hits()
        .hits()
        .forEach(
            searchHit ->
                iCal.addEvent(
                    ConversionUtil.toVEvent(
                        searchHit,
                        configuration.getDefaultLocale(),
                        configuration.getGbifPortalUrl() + configuration.getEsEventsIndex())));
    return Biweekly.write(iCal).go();
  }

  /**
   * Upcoming events RSS feed.
   */
  @GetMapping(path = "events/upcoming.xml", produces = MediaType.APPLICATION_ATOM_XML_VALUE)
  public String getUpComingEvents(@RequestParam(value = "limit", required = false) Integer limit) {
    return toXmlAtomFeed(
        newEventsFeed(), UPCOMING_EVENTS, START_FIELD, configuration.getEsEventsIndex(), limit);
  }

  /**
   * Single event RSS feed in Atom format.
   */
  @GetMapping(path = "events/{eventId}", produces = MEDIA_TYPE_ICAL)
  public ResponseEntity<String> getEvent(@PathVariable("eventId") String eventId) {
    try {
      GetResponse<Map> response = esClient.get(
          GetRequest.of(g -> g.index(configuration.getEsEventsIndex()).id(eventId)),
          Map.class);

      return Optional.ofNullable(
              ConversionUtil.toVEvent(
                  response,
                  configuration.getDefaultLocale(),
                  configuration.getGbifPortalUrl() + configuration.getEsEventsIndex()))
          .map(
              event -> {
                ICalendar iCal = new ICalendar();
                iCal.addEvent(event);
                return ResponseEntity.ok(Biweekly.write(iCal).go());
              })
          .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * News RSS feeds.
   */
  @GetMapping(path = "news/rss", produces = MediaType.APPLICATION_ATOM_XML_VALUE)
  public String getNews(@RequestParam(value = "limit", required = false) Integer limit) {
    return toXmlAtomFeed(
        newNewsFeed(), null, CREATED_AT_FIELD, configuration.getEsNewsIndex(), limit);
  }

  /**
   * New RSS feed for GBIF region.
   */
  @GetMapping(path = "news/rss/{gbifRegion}", produces = MediaType.APPLICATION_ATOM_XML_VALUE)
  public String getNewsByRegion(
      @PathVariable("gbifRegion") String region,
      @RequestParam(value = "limit", required = false) Integer limit) {
    return toXmlAtomFeed(
        newNewsFeed(),
        Query.of(q -> q.term(t -> t.field(GBIF_REGION_FIELD).value(region))),
        CREATED_AT_FIELD,
        configuration.getEsNewsIndex(),
        limit);
  }

  /**
   * News RSS feed for a program and language.
   */
  @GetMapping(
      path = "news/rss/{acronym}/{language}",
      produces = MediaType.APPLICATION_ATOM_XML_VALUE)
  public String getProgramNews(
      @PathVariable("acronym") String acronym,
      @PathVariable("language") String language,
      @RequestParam(value = "limit", required = false) Integer limit) {
    return toXmlAtomFeed(
        newNewsFeed(),
        Query.of(q -> q.term(t -> t.field("programmeTag").value(findProgrammeId(acronym)))),
        CREATED_AT_FIELD,
        configuration.getEsNewsIndex(),
        getLocale(language),
        limit);
  }

  /**
   * JSON News for a program and language.
   */
  @GetMapping(path = "news/json/{acronym}/{language}", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<SyndEntry> getProgrammeNewsJson(
      @PathVariable("acronym") String acronym,
      @PathVariable("language") String language,
      @RequestParam(value = "limit", required = false) Integer limit) {
    return executeQuery(
            Query.of(q -> q.term(t -> t.field("programmeTag").value(findProgrammeId(acronym)))),
            CREATED_AT_FIELD,
            configuration.getEsNewsIndex(),
            limit)
        .hits()
        .hits()
        .stream()
        .map(
            searchHit ->
                ConversionUtil.toFeedEntry(
                    searchHit,
                    getLocale(language),
                    configuration.getGbifPortalUrl() + configuration.getEsNewsIndex()))
        .toList();
  }

  /**
   * Data uses RSS feed.
   */
  @GetMapping(path = "uses/rss", produces = MediaType.APPLICATION_ATOM_XML_VALUE)
  public String getDataUses(@RequestParam(value = "limit", required = false) Integer limit) {
    return toXmlAtomFeed(
        newNewsFeed(), null, CREATED_AT_FIELD, configuration.getEsDataUseIndex(), limit);
  }

  /**
   * Parse language into a Locale.
   */
  private String getLocale(String language) {
    try {
      Optional<String> optLanguage = Optional.ofNullable(language);
      optLanguage.ifPresent(
          languageLocale -> {
            String localeStr = HYPHEN.matcher(language).replaceAll("_");
            Locale.forLanguageTag(localeStr);
          });
      return optLanguage.orElse(configuration.getDefaultLocale());
    } catch (IllegalArgumentException ex) {
      LOG.error("Error generating locale", ex);
      throw new WebApplicationException(
          String.format("Language %s is not supported", language), HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * Builds a programme:id query builder.
   */
  private Query programmeQuery(String acronym) {
    return Query.of(q -> q.term(t -> t
      .field("programmeTag")
      .value(findProgrammeId(acronym))
    ));
  }

  /**
   * Finds the programme id by its acronym.
   */
  private String findProgrammeId(String acronym) {
    SearchResponse<Map> response =
        executeQuery(
            Query.of(q -> q.term(t -> t.field("acronym").value(acronym))),
            CREATED_AT_FIELD,
            configuration.getEsProgrammeIndex(),
            1);
    return response.hits().hits().stream()
        .map(Hit::id)
        .findFirst()
        .orElseThrow(
            () ->
                new WebApplicationException(
                    String.format("Project acronym %s not found", acronym),
                    HttpStatus.BAD_REQUEST));
  }

  /**
   * Executes a query and translates the results into XML Atom Feeds.
   */
  private String toXmlAtomFeed(
      SyndFeed feed, Query filter, String dateSortField, String idxName, Integer limit) {
    return toXmlAtomFeed(
        feed, filter, dateSortField, idxName, configuration.getDefaultLocale(), limit);
  }

  /**
   * Executes a query and translates the results into XML Atom Feeds.
   */
  private String toXmlAtomFeed(
      SyndFeed feed,
      Query filter,
      String dateSortField,
      String idxName,
      String locale,
      Integer limit) {
    try {
      feed.setEntries(
          executeQuery(filter, dateSortField, idxName, limit).hits().hits().stream()
              .map(
                  searchHit ->
                      ConversionUtil.toFeedEntry(
                          searchHit, locale, configuration.getGbifPortalUrl() + idxName))
              .toList());
      StringWriter writer = new StringWriter();
      new SyndFeedOutput().output(feed, writer);
      return writer.toString();
    } catch (IOException | FeedException ex) {
      LOG.error("Error generating events RSS feed", ex);

      throw new WebApplicationException(
          "Error generating events RSS feed", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Executes the default search query.
   */
  private SearchResponse<Map> executeQuery(
      String query, Query filter, String dateSortField, String idxName, Integer limit) {
    try {
      BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder()
          .filter(SEARCHABLE);

      if (query == null) {
        boolQueryBuilder.must(MatchAllQuery.of(m -> m));
      } else {
        boolQueryBuilder.must(Query.of(q -> q.wrapper(w -> w.query(query))));
      }
      Optional.ofNullable(filter).ifPresent(boolQueryBuilder::filter);

      return esClient.search(
          SearchRequest.of(s -> s
              .index(idxName)
              .query(boolQueryBuilder.build())
              .sort(sort -> sort.field(f -> f.field(dateSortField).order(SortOrder.Desc)))
              .size(getLimit(limit))),
          Map.class);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private int getLimit(Integer limit) {
    return Math.min(Optional.ofNullable(limit).orElse(DEFAULT_SIZE), MAX_SIZE);
  }

  /**
   * Executes the default search query.
   */
  private SearchResponse<Map> executeQuery(
      Query filter, String dateSortField, String idxName, Integer limit) {
    return executeQuery(null, filter, dateSortField, idxName, limit);
  }
}
