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

import org.gbif.content.service.WebHookRequest;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;

import com.google.common.io.Resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test cases for the WebHookRequest class.
 */
public class WebHookRequestTest {

  public static final String REQUEST_TEST_FILE = "webhookrequest.json";

  /**
   * Tests that the WebHook request can be deserialized.
   */
  @Test
  public void testWebSerialization() throws IOException {
    WebHookRequest webHookRequest = WebHookRequest.fromRequest(getMockRequest());
    assertEquals(WebHookRequest.Topic.EntryPublish, webHookRequest.getTopic());
    assertEquals("DataUse", webHookRequest.getContentTypeId());
    assertEquals("83217", webHookRequest.getId());
    assertEquals("Entry", webHookRequest.getType());
  }

  /**
   * Creates a HttpServletRequest based on the REQUEST_TEST_FILE.
   */
  private static HttpServletRequest getMockRequest() throws IOException {
    HttpServletRequest mockHttpServletRequest = mock(HttpServletRequest.class);
    when(mockHttpServletRequest.getHeader(WebHookRequest.CONTENTFUL_TOPIC_HEADER))
        .thenReturn(WebHookRequest.Topic.EntryPublish.getValue());
    when(mockHttpServletRequest.getInputStream())
        .thenReturn(
            new DelegatingServletInputStream(
                Resources.getResource(REQUEST_TEST_FILE).openStream()));
    return mockHttpServletRequest;
  }
}
