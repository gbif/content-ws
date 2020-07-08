package org.gbif.content.resource;

import org.elasticsearch.client.Client;
import org.gbif.content.ContentWsApplication;
import org.gbif.content.config.ContentWsConfigurationProperties;
import org.gbif.content.security.SyncAuthenticationFilter;
import org.gbif.content.service.JenkinsJobClient;
import org.gbif.content.service.WebHookRequest;
import org.gbif.content.utils.Paths;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

  //Mock values of Jenkins URLs, both are not really contacted, they can point to nonexistent urls.
  private static final String JENKINS_TEST_URL = "http://builds.gbif.org/job/run-content-crawler-fake/";
  private static final String LOCATION_TEST = JENKINS_TEST_URL + "80/";

  private MockMvc mockMvc;

  @MockBean
  private JenkinsJobClient jenkinsJobClientMock;

  @Autowired
  private Client searchIndex;

  @Autowired
  private ContentWsConfigurationProperties properties;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    this.mockMvc = MockMvcBuilders.standaloneSetup(
        new SyncResource(jenkinsJobClientMock, searchIndex, properties))
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
            .content("content")
            .contentType(SyncResource.CONTENTFUL_CONTENT_TYPE)
            .header(HttpHeaders.AUTHORIZATION, getAuthCredentials())
            .header(WebHookRequest.CONTENTFUL_TOPIC_HEADER, WebHookRequest.Topic.EntryPublish.getValue())
    ).andExpect(status().isAccepted());
  }
}
