package org.gbif.content.resource;

import com.google.common.io.Resources;
import org.gbif.content.service.WebHookRequest;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

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
      .thenReturn(new DelegatingServletInputStream(Resources.getResource(REQUEST_TEST_FILE).openStream()));
    return mockHttpServletRequest;
  }
}
