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
package org.gbif.content.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import org.jsoup.Jsoup;

import com.google.common.base.Strings;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

import biweekly.component.VEvent;

import static org.gbif.content.utils.SearchFieldsUtils.getDateField;
import static org.gbif.content.utils.SearchFieldsUtils.getField;
import static org.gbif.content.utils.SearchFieldsUtils.getLinkUrl;
import static org.gbif.content.utils.SearchFieldsUtils.getLocationField;

/**
 * Utility class to convert search results into RSS feed and iCal entries.
 */
public class ConversionUtil {

  private static final Parser MARKDOWN_PARSER = Parser.builder().build();

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern(
          "[yyyy-MM-dd'T'HH:mm:ssXXX][yyyy-MM-dd'T'HH:mmXXX][yyyy-MM-dd'T'HH:mm:ss.SSS XXX][yyyy-MM-dd'T'HH:mm:ss.SSSXXX][yyyy-MM-dd'T'HH:mm:ssZ]"
              + "[yyyy-MM-dd'T'HH:mm:ss.SSSSSS][yyyy-MM-dd'T'HH:mm:ss.SSSSS][yyyy-MM-dd'T'HH:mm:ss.SSSS][yyyy-MM-dd'T'HH:mm:ss.SSS]"
              + "[yyyy-MM-dd'T'HH:mm:ss][yyyy-MM-dd'T'HH:mm:ss XXX][yyyy-MM-dd'T'HH:mm:ssXXX][yyyy-MM-dd'T'HH:mm:ss]"
              + "[yyyy-MM-dd'T'HH:mm][yyyy-MM-dd][yyyy-MM][yyyy]");

  static final Function<String, Date> STRING_TO_DATE =
      dateAsString -> {
        if (Strings.isNullOrEmpty(dateAsString)) {
          return null;
        }

        boolean firstYear = false;
        if (dateAsString.startsWith("0000")) {
          firstYear = true;
          dateAsString = dateAsString.replaceFirst("0000", "1970");
        }

        // parse string
        TemporalAccessor temporalAccessor =
            FORMATTER.parseBest(
                dateAsString,
                ZonedDateTime::from,
                LocalDateTime::from,
                LocalDate::from,
                YearMonth::from,
                Year::from);
        Date dateParsed = null;
        if (temporalAccessor instanceof ZonedDateTime zonedDateTime) {
          dateParsed = Date.from((zonedDateTime).toInstant());
        } else if (temporalAccessor instanceof LocalDateTime localDateTime) {
          dateParsed = Date.from((localDateTime).toInstant(ZoneOffset.UTC));
        } else if (temporalAccessor instanceof LocalDate localDate) {
          dateParsed =
              Date.from(((localDate).atStartOfDay()).toInstant(ZoneOffset.UTC));
        } else if (temporalAccessor instanceof YearMonth yearMonth) {
          dateParsed =
              Date.from(
                  ((yearMonth).atDay(1))
                      .atStartOfDay()
                      .toInstant(ZoneOffset.UTC));
        } else if (temporalAccessor instanceof Year year) {
          dateParsed =
              Date.from(
                  ((year).atDay(1)).atStartOfDay().toInstant(ZoneOffset.UTC));
        }

        if (dateParsed != null && firstYear) {
          Calendar cal = Calendar.getInstance();
          cal.setTime(dateParsed);
          cal.set(Calendar.YEAR, 1);
          return cal.getTime();
        }

        return dateParsed;
      };

  /**
   * Private constructor.
   */
  private ConversionUtil() {
    // NOP
  }

  /**
   * Transforms a SearchHit into a SyndEntry instance.
   */
  public static SyndEntry toFeedEntry(Hit<Map> searchHit, String locale, String altBaseLink) {
    SyndEntry entry = new SyndEntryImpl();
    Map<String, Object> source = (Map<String, Object>) searchHit.source();
    getField(source, "title", locale)
        .ifPresent(
            title ->
                entry.setTitle(
                    HtmlRenderer.builder().build().render(MARKDOWN_PARSER.parse(title))));
    SyndContent description = new SyndContentImpl();
    description.setType("text/html");
    getField(source, "body", locale)
        .ifPresent(
            body ->
                description.setValue(
                    HtmlRenderer.builder().build().render(MARKDOWN_PARSER.parse(body))));
    entry.setDescription(description);
    entry.setLink(altBaseLink + '/' + searchHit.id());
    entry.setPublishedDate(parseDate((String) source.get("createdAt")));
    return entry;
  }

  /**
   * Transforms a SearchHit into a VEvent instance.
   */
  public static VEvent toVEvent(Hit<Map> searchHit, String locale, String altBaseLink) {
    Map<String, Object> source = (Map<String, Object>) searchHit.source();
    return toVEvent(
        Optional.ofNullable(source.get("id")).map(id -> (String) id).orElse(""),
        searchHit.source(),
        locale,
        altBaseLink);
  }

  /**
   * Converts a ElasticSearch GetResponse into a VEvent instance to be used in an iCal feed.
   */
  public static VEvent toVEvent(GetResponse<Map> getResponse, String locale, String altBaseLink) {
    if (getResponse.found()) {
      return toVEvent(getResponse.id(), getResponse.source(), locale, altBaseLink);
    }
    return null;
  }

  private static VEvent toVEvent(
      String id, Map source, String locale, String altBaseLink) {
    HtmlToPlainText formatter = new HtmlToPlainText();
    VEvent vEvent = new VEvent();
    vEvent.setUid(id);
    Map<String, Object> sourceMap = (Map<String, Object>) source;
    getField(sourceMap, "title", locale)
        .ifPresent(
            title ->
                vEvent.setSummary(
                    formatter.getPlainText(
                        Jsoup.parse(
                            HtmlRenderer.builder().build().render(MARKDOWN_PARSER.parse(title))))));
    getField(sourceMap, "body", locale)
        .ifPresent(
            body ->
                vEvent.setDescription(
                    formatter.getPlainText(
                        Jsoup.parse(
                            HtmlRenderer.builder().build().render(MARKDOWN_PARSER.parse(body))))));
    getLinkUrl(sourceMap, "primaryLink", locale).ifPresent(vEvent::setUrl);
    vEvent.setUrl(altBaseLink + '/' + id);
    getLocationField(sourceMap, "coordinates").ifPresent(vEvent::setLocation);
    getDateField(sourceMap, "start").ifPresent(vEvent::setDateStart);
    getDateField(sourceMap, "end").ifPresent(vEvent::setDateEnd);
    return vEvent;
  }

  public static Date parseDate(String date) {
    return STRING_TO_DATE.apply(date);
  }
}
