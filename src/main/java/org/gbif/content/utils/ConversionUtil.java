package org.gbif.content.utils;

import java.util.Map;

import biweekly.component.VEvent;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import org.elasticsearch.search.SearchHit;

import static org.elasticsearch.index.mapper.DateFieldMapper.DEFAULT_DATE_TIME_FORMATTER;

import static org.gbif.content.utils.SearchFieldsUtils.getDateField;
import static org.gbif.content.utils.SearchFieldsUtils.getField;

/**
 * Utility class to convert search results into RSS feed and iCal entries.
 */
public class ConversionUtil {

  /**
   * Private constructor.
   */
  private ConversionUtil() {
    // NOP
  }


  /**
   * Transforms a SearchHit into a SyndEntry instance.
   */
  public static SyndEntry toFeedEntry(SearchHit searchHit) {
    SyndEntry entry = new SyndEntryImpl();
    Map<String,Object> source = searchHit.getSource();
    String locale = (String)searchHit.getSource().get("locale");
    getField(source, "title", locale).ifPresent(entry::setTitle);
    SyndContent description = new SyndContentImpl();
    getField(source, "body", locale).ifPresent(description::setValue);
    entry.setDescription(description);
    getField(source, "primaryLink", locale).ifPresent(entry::setLink);
    entry.setPublishedDate(DEFAULT_DATE_TIME_FORMATTER.parser()
                             .parseDateTime((String)source.get("createdAt")).toDate());
    return entry;
  }

  /**
   * Transforms a SearchHit into a VEvent instance.
   */
  public static VEvent toVEvent(SearchHit searchHit) {
    VEvent vEvent = new VEvent();
    Map<String,Object> source = searchHit.getSource();
    String locale = (String)searchHit.getSource().get("locale");
    getField(source, "title", locale).ifPresent(vEvent::setSummary);
    getField(source, "body", locale).ifPresent(vEvent::setDescription);
    getField(source, "primaryLink", locale).ifPresent(vEvent::setUrl);
    getField(source, "id", locale).ifPresent(vEvent::setUid);
    getField(source, "coordinates", locale).ifPresent(vEvent::setLocation);
    getDateField(source, "start", locale).ifPresent(vEvent::setDateStart);
    getDateField(source, "end", locale).ifPresent(vEvent::setDateEnd);
    return vEvent;
  }

}
