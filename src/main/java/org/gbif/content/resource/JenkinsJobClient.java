package org.gbif.content.resource;

import org.gbif.content.conf.ContentWsConfiguration;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.http.client.utils.URIBuilder;

/**
 * Utility class that wraps the connection and interaction against  a Jenkins job.
 */
public class JenkinsJobClient {

  private final String jenkinsJobUrl;

  /**
   * @param jenkinsJobUrl url to the Jenkins job
   */
  public JenkinsJobClient(String jenkinsJobUrl) {
    this.jenkinsJobUrl = jenkinsJobUrl;
  }

  /**
   * Executes the Jenkins Job.
   */
  public Response execute(String environment) throws IOException {
    HttpURLConnection connection=  null;
    try {
      connection = (HttpURLConnection)jobUrl(environment).openConnection();
      Response.Status jenkinsJobStatus = Response.Status.fromStatusCode(connection.getResponseCode());
      if(Response.Status.Family.INFORMATIONAL == jenkinsJobStatus.getFamily()
         || Response.Status.Family.SUCCESSFUL == jenkinsJobStatus.getFamily()) {
        return Response.status(Response.Status.ACCEPTED)
          .header(HttpHeaders.LOCATION, Optional
            .ofNullable(connection.getHeaderField(HttpHeaders.LOCATION)).orElse(""))
          .build();
      }
      return Response.status(jenkinsJobStatus).build();
    } finally {
      Optional.ofNullable(connection).ifPresent(HttpURLConnection::disconnect);
    }
  }

  /**
   * Builds an URL to connect against the Jenkins job.
   */
  private URL jobUrl(String environment) {
    try {
      URIBuilder builder = new URIBuilder(jenkinsJobUrl);
      builder.addParameter(ContentWsConfiguration.Synchronization.JenkinsJob.ENV_PARAM, environment);
      return builder.build().toURL();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

}
