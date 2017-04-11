package org.gbif.content.utils;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

import static org.elasticsearch.index.mapper.DateFieldMapper.DEFAULT_DATE_TIME_FORMATTER;

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
  public static Optional<String> getField(Map<String,Object> source, String field, String locale) {
    return Optional.ofNullable(source.get(field)).map(value -> ((Map<String,String>)value).get(locale));
  }


  /**
   * Extracts the field date value from the source map for a specific locale.
   */
  public static Optional<Date> getDateField(Map<String,Object> source, String field, String locale) {
    return getField(source, field, locale)
      .map(value -> DEFAULT_DATE_TIME_FORMATTER.parser().parseDateTime(value).toDate());
  }
}
