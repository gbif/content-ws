package org.gbif.content.resource;

import org.gbif.content.conf.ContentWsConfiguration;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.Principal;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Resources;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.elasticsearch.client.Client;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.ClassRule;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the SyncResource.
 */
public class SyncResourceTest {

  private static final String AUTH_HEADER = "Authorization";

  //Mock values of Jenkins URLs, both are not really contacted, they can point to nonexistent urls.
  private static final String JENKINS_TEST_URL  = "http://builds.gbif.org/job/run-content-crawler-fake/";
  private static final String LOCATION_TEST = JENKINS_TEST_URL + "80/";

  //Static instance of the configuration settings loaded from the test file
  private static ContentWsConfiguration configuration;

  /**
   * Lazy loading of configuration test file.
   */
  private static ContentWsConfiguration getConfiguration() {
    if (configuration == null) {
      configuration = ContentWsConfigurationTest.getTestConfiguration();
    }
    return configuration;
  }

  //Grizzly is required since the in-memory Jersey test container does not support all features,
  // such as the @Context injection used by BasicAuthFactory and OAuthFactory.
  @ClassRule
  public static ResourceTestRule resource = ResourceTestRule.builder()
    .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
    //Authentication
    .addProvider(new AuthDynamicFeature(SyncAuthenticator
                                          .buildAuthFilter(getConfiguration().getSynchronization().getToken())))
    .addProvider(new AuthValueFactoryProvider.Binder<>(Principal.class))
    //Test resource
    .addResource(new SyncResource(getJenkinsJobUrlMock(), mock(Client.class)))
    .build();

  /**
   * Creates a mock instance of URL that doesn't trigger any remote call.
   */
  private static URL getJenkinsJobUrlMock() {
    try {

      HttpURLConnection mockConnection = mock(HttpURLConnection.class);
      when(mockConnection.getResponseCode()).thenReturn(Response.Status.ACCEPTED.getStatusCode());

      when(mockConnection.getHeaderField(HttpHeaders.LOCATION)).thenReturn(LOCATION_TEST);
      //mocking httpconnection by URLStreamHandler since we can not mock URL class.
      URLStreamHandler stubURLStreamHandler = new URLStreamHandler() {
        @Override
        protected URLConnection openConnection(URL u ) {
          return mockConnection ;
        }
      };
      return new URL(null, JENKINS_TEST_URL, stubURLStreamHandler);
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Builds the test credentials.
   */
  private static String getAuthCredentials() {
    return SyncAuthenticator.BEARER_PREFIX + " " + getConfiguration().getSynchronization().getToken();
  }

  /**
   * Reads the content of the test file into a String.
   */
  private static String getTestContent() throws IOException {
    return CharStreams.toString(new InputStreamReader(Resources.getResource(WebHookRequestTest.REQUEST_TEST_FILE)
                                                        .openStream(), Charsets.UTF_8));
  }

  /**
   * Test a synchronization call.
   */
  @Test
  public void testSync() throws IOException {
   Response response = resource.getJerseyTest().target(Paths.SYNC_RESOURCE_PATH)
                        .request()
                        .header(AUTH_HEADER, getAuthCredentials())
                        .header(WebHookRequest.CONTENTFUL_TOPIC_HEADER, WebHookRequest.Topic.EntryPublish.getValue())
                        .post(Entity.entity(getTestContent(), SyncResource.CONTENTFUL_CONTENT_TYPE));
   assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
  }

}
