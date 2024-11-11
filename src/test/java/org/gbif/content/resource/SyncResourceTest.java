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

import org.gbif.content.ContentWsApplication;
import org.gbif.content.config.ContentWsProperties;
import org.gbif.content.security.SyncAuthenticationFilter;
import org.gbif.content.service.JenkinsJobClient;
import org.gbif.content.service.WebHookRequest;
import org.gbif.content.utils.Paths;

import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.autoconfigure.elasticsearch.ElasticSearchRestHealthContributorAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
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
    classes = {ContentWsApplication.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = ElasticSearchRestHealthContributorAutoConfiguration.class)
public class SyncResourceTest {

  private MockMvc mockMvc;

  @Autowired
  @Qualifier("searchClient")
  private RestHighLevelClient searchIndex;

  @Autowired private ContentWsProperties properties;


  @BeforeEach
  public void setup() {
    MockitoAnnotations.initMocks(this);
    this.mockMvc =
        MockMvcBuilders.standaloneSetup(new SyncResource(jenkinsJobMock(), searchIndex, properties))
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
    mockMvc
        .perform(
            post(Paths.SYNC_RESOURCE_PATH)
                .param("env", "dev")
                .content(
                    "{"
                        + "\"sys\": {\n"
                        + "      \"type\": \"Entry\",\n"
                        + "     \"contentType\": {\n"
                        + "        \"sys\": {\n"
                        + "           \"type\": \"Link\",\n"
                        + "            \"linkType\": \"ContentType\",\n"
                        + "           \"id\": \"DataUse\"\n"
                        + "        }\n"
                        + "      },\n"
                        + "      \"id\": \"82531\"\n"
                        + "   }"
                        + "}")
                .contentType(SyncResource.CONTENTFUL_CONTENT_TYPE)
                .header(HttpHeaders.AUTHORIZATION, getAuthCredentials())
                .header(
                    WebHookRequest.CONTENTFUL_TOPIC_HEADER,
                    WebHookRequest.Topic.EntryPublish.getValue()))
        .andExpect(status().isAccepted());
  }
}
