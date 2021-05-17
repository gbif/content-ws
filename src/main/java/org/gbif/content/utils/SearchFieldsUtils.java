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

import java.util.Date;
import java.util.Map;
import java.util.Optional;

/**
 * Utility class to get fields from search responses.
 */
public class SearchFieldsUtils {

  /**
   * Private constructor.
   */
  private SearchFieldsUtils() {
    // NOP
  }

  /**
   * Extracts the field value from the source map for a specific locale.
   */
  public static Optional<String> getField(Map<String, Object> source, String field, String locale) {
    return Optional.ofNullable(source.get(field))
        .map(value -> ((Map<String, String>) value).get(locale));
  }

  /**
   * Extracts the field value from the source map for a specific locale.
   */
  public static Optional<String> getField(Map<String, Object> source, String field) {
    return Optional.ofNullable(source.get(field)).map(value -> (String) value);
  }

  /**
   * Extracts the a location latn/log field value from the source map.
   * The output is formatted according to https://tools.ietf.org/html/rfc5545#page-87.
   */
  public static Optional<String> getLocationField(Map<String, Object> source, String field) {
    return Optional.ofNullable(source.get(field))
        .map(
            value -> {
              Map<String, Double> coordinates = (Map<String, Double>) value;
              return coordinates.get("lat").toString() + ';' + coordinates.get("lon").toString();
            });
  }

  /**
   * Extracts the field value from the source map for a specific locale.
   */
  public static Optional<String> getNestedField(Map<String, Object> source, String... field) {
    Optional<Map<String, ?>> value = Optional.ofNullable((Map<String, ?>) source.get(field[0]));
    for (int i = 1; i < field.length - 1; i++) {
      if (value.isPresent()) {
        value = Optional.ofNullable((Map<String, ?>) value.get().get(field[i]));
      } else {
        return Optional.empty();
      }
    }
    return Optional.ofNullable(value.get().get(field[field.length - 1])).map(o -> (String) o);
  }

  /**
   * Extracts the field date value from the source map for a specific locale.
   */
  public static Optional<Date> getDateField(Map<String, Object> source, String field) {
    return getField(source, field)
        .map(ConversionUtil::parseDate);
  }

  /**
   * Gets the url of Link element.
   */
  public static Optional<String> getLinkUrl(
      Map<String, Object> source, String field, String locale) {
    return Optional.ofNullable(source.get(field))
        .map(link -> (Map<String, Object>) link)
        .map(link -> (Map<String, Object>) link.get("url"))
        .map(url -> (String) url.get(locale));
  }
}
