package org.gbif.content.resource;

import org.elasticsearch.client.Client;
import org.gbif.content.ContentWsApplication;
import org.gbif.content.config.ContentWsConfigurationProperties;
import org.gbif.content.security.SyncAuthenticationFilter;
import org.gbif.content.service.JenkinsJobClient;
import org.gbif.content.service.WebHookRequest;
import org.gbif.content.utils.Paths;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for the SyncResource.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(
    classes = ContentWsApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = ElasticsearchAutoConfiguration.class)
public class SyncResourceTest {

  private MockMvc mockMvc;

  @Autowired
  @Qualifier("searchClient")
  private Client searchIndex;

  @Autowired
  private ContentWsConfigurationProperties properties;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.initMocks(this);
    this.mockMvc = MockMvcBuilders.standaloneSetup(
        new SyncResource(jenkinsJobMock(), searchIndex, properties))
        .addFilters(new SyncAuthenticationFilter(properties.getSynchronization().getToken()))
        .build();
  }

  /**
   * Creates a mock instance of URL that doesn't trigger any remote call.
   */
  private static JenkinsJobClient jenkinsJobMock() {
    JenkinsJobClient jenkinsJobClient = mock(JenkinsJobClient.class);
    when(jenkinsJobClient.execute("dev")).thenReturn(ResponseEntity.accepted().build());
    return jenkinsJobClient;
  }

  /**
   * Builds the test credentials.
   */
  private String getAuthCredentials() {
    return "Bearer " + properties.getSynchronization().getToken();
  }

  /**
   * Test a synchronization call.
   */
  @Test
  public void testSync() throws Exception {
    mockMvc.perform(
        post(Paths.SYNC_RESOURCE_PATH)
            .param("env", "dev")
            .content("{" +
                "\"sys\": {\n" +
                "      \"type\": \"Entry\",\n" +
                "     \"contentType\": {\n" +
                "        \"sys\": {\n" +
                "           \"type\": \"Link\",\n" +
                "            \"linkType\": \"ContentType\",\n" +
                "           \"id\": \"DataUse\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"id\": \"82531\"\n" +
                "   }" +
                "}")
            .contentType(SyncResource.CONTENTFUL_CONTENT_TYPE)
            .header(HttpHeaders.AUTHORIZATION, getAuthCredentials())
            .header(WebHookRequest.CONTENTFUL_TOPIC_HEADER, WebHookRequest.Topic.EntryPublish.getValue())
    ).andExpect(status().isAccepted());
  }
}
