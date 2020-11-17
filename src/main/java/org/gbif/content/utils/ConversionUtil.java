/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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

import java.util.Map;
import java.util.Optional;

import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.search.SearchHit;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

import biweekly.component.VEvent;

import static org.elasticsearch.index.mapper.DateFieldMapper.DEFAULT_DATE_TIME_FORMATTER;
import static org.gbif.content.utils.SearchFieldsUtils.getDateField;
import static org.gbif.content.utils.SearchFieldsUtils.getField;
import static org.gbif.content.utils.SearchFieldsUtils.getLinkUrl;
import static org.gbif.content.utils.SearchFieldsUtils.getLocationField;
import static org.gbif.content.utils.SearchFieldsUtils.getNestedField;

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
    Map<String, Object> source = searchHit.getSourceAsMap();
    getField(source, "title", locale).ifPresent(entry::setTitle);
    SyndContent description = new SyndContentImpl();
    description.setType("text/html");
    getField(source, "body", locale)
        .ifPresent(
            body ->
                description.setValue(
                    HtmlRenderer.builder().build().render(MARKDOWN_PARSER.parse(body))));
    entry.setDescription(description);
    entry.setLink(
        getNestedField(source, "primaryLink", "url", locale)
            .orElseGet(() -> altBaseLink + '/' + searchHit.getId()));
    entry.setPublishedDate(
        DEFAULT_DATE_TIME_FORMATTER.parseJoda((String) source.get("createdAt")).toDate());
    return entry;
  }

  /**
   * Transforms a SearchHit into a VEvent instance.
   */
  public static VEvent toVEvent(SearchHit searchHit, String locale) {
    Map<String, Object> source = searchHit.getSourceAsMap();
    return toVEvent(
        Optional.ofNullable(source.get("id")).map(id -> (String) id).orElse(""),
        searchHit.getSourceAsMap(),
        locale);
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
