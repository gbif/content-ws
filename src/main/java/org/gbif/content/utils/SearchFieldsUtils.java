package org.gbif.content.utils;

import java.util.Arrays;
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
    return Optional.ofNullable(source.get(field)).map(value -> ((Map<String, String>) value).get(locale));
  }

  /**
   * Extracts the field value from the source map for a specific locale.
   */
  public static Optional<String> getField(Map<String,Object> source, String field) {
    return Optional.ofNullable(source.get(field)).map(value -> ((String) value));
  }

  /**
   * Extracts the field value from the source map for a specific locale.
   */
  public static Optional<String> getNestedField(Map<String,Object> source, String...field) {
    Optional<Map<String, ?>> value = Optional.ofNullable((Map<String,?>)source.get(field[0]));
    for (int i = 1; i < field.length - 1; i++) {
      if (value.isPresent()) {
        value = Optional.ofNullable((Map<String,?>)value.get().get(field[i]));
      } else {
        return Optional.empty();
      }
    }
    return Optional.ofNullable(value.get().get(field[field.length -1])).map(o -> (String)o);
  }


  /**
   * Extracts the field date value from the source map for a specific locale.
   */
  public static Optional<Date> getDateField(Map<String,Object> source, String field) {
    return getField(source, field)
      .map(value -> DEFAULT_DATE_TIME_FORMATTER.parser().parseDateTime(value).toDate());
  }
}
