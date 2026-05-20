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

import java.time.Instant;
import java.time.format.DateTimeParseException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConversionUtilTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "2026-09-07T00:00+0000",
        "2026-09-07T00:00+00:00",
        "2026-09-07T00:00:00+00:00",
        "2026-09-07T00:00:00Z",
        "2026-09-07",
        "2026-09",
        "2026"
      })
  void parseDateSupportsCommonEventFormats(String date) {
    assertNotNull(ConversionUtil.parseDate(date));
  }

  @Test
  void parseDateCompactOffsetWithoutSeconds() {
    assertEquals(
        Instant.parse("2026-09-07T00:00:00Z"),
        ConversionUtil.parseDate("2026-09-07T00:00+0000").toInstant());
  }

  @Test
  void parseDateRejectsUnknownFormat() {
    assertThrows(DateTimeParseException.class, () -> ConversionUtil.parseDate("not-a-date"));
  }

  @Test
  void sanitizeIcalTextTrimsAndCollapsesNewlines() {
    assertEquals(
        "10th International Barcode of life",
        ConversionUtil.sanitizeIcalText("\n10th International Barcode of life\n"));
    assertEquals(
        "First paragraph. Second paragraph.",
        ConversionUtil.sanitizeIcalText("First paragraph.\n\nSecond paragraph.\n"));
  }
}
