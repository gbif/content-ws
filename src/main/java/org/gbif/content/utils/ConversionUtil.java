package org.gbif.content.utils;

import java.util.Map;
import java.util.Optional;

import biweekly.component.VEvent;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.search.SearchHit;

import static org.elasticsearch.index.mapper.DateFieldMapper.DEFAULT_DATE_TIME_FORMATTER;

import static org.gbif.content.utils.SearchFieldsUtils.getDateField;
import static org.gbif.content.utils.SearchFieldsUtils.getField;
import static org.gbif.content.utils.SearchFieldsUtils.getNestedField;
import static org.gbif.content.utils.SearchFieldsUtils.getLocationField;
import static org.gbif.content.utils.SearchFieldsUtils.getLinkUrl;

/**
 * Utility class to convert search results into RSS feed and iCal entries.
 */
public class ConversionUtil {

  private static final Parser MARKDOWN_PARSER = Parser.builder().build();

  /**
   * Private constructor.
   */
  private ConversionUtil() {
    // NOP
  }

  /**
   * Transforms a SearchHit into a SyndEntry instance.
   */
  public static SyndEntry toFeedEntry(SearchHit searchHit, String locale, String altBaseLink) {
    SyndEntry entry = new SyndEntryImpl();
    Map<String,Object> source = searchHit.getSource();
    getField(source, "title", locale).ifPresent(entry::setTitle);
    SyndContent description = new SyndContentImpl();
    description.setType("text/html");
    getField(source, "body", locale)
      .ifPresent(body ->
                   description.setValue(HtmlRenderer.builder().build().render(MARKDOWN_PARSER.parse(body))));
    entry.setDescription(description);
    entry.setLink(getNestedField(source, "primaryLink", "url", locale)
                    .orElseGet(() -> altBaseLink + '/' + searchHit.id()));
    entry.setPublishedDate(DEFAULT_DATE_TIME_FORMATTER.parser()
                             .parseDateTime((String)source.get("createdAt")).toDate());
    return entry;
  }

  /**
   * Transforms a SearchHit into a VEvent instance.
   */
  public static VEvent toVEvent(SearchHit searchHit, String locale) {
    Map<String,Object> source = searchHit.getSource();
    return toVEvent(Optional.ofNullable(source.get("id")).map(id -> (String)id).orElse(""), searchHit.getSource(), locale);
  }


  /**
   * Converts a ElasticSearch GetResponse into a VEvent instance to be used in an iCal feed.
   */
  public static VEvent toVEvent(GetResponse getResponse, String locale) {
    return toVEvent(getResponse.getId(), getResponse.getSource(), locale);
  }

  private static VEvent toVEvent(String id, Map<String, Object> source, String locale) {
    VEvent vEvent = new VEvent();
    vEvent.setUid(id);
    getField(source, "title", locale).ifPresent(vEvent::setSummary);
    getField(source, "body", locale).ifPresent(vEvent::setDescription);
    getLinkUrl(source, "primaryLink", locale).ifPresent(vEvent::setUrl);
    getLocationField(source, "coordinates").ifPresent(vEvent::setLocation);
    getDateField(source, "start").ifPresent(vEvent::setDateStart);
    getDateField(source, "end").ifPresent(vEvent::setDateEnd);
    return vEvent;
  }

}
