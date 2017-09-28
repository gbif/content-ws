package org.gbif.content.resource;

import org.gbif.content.conf.ContentWsConfiguration;
import org.gbif.content.conf.ContentWsConfiguration.Synchronization;
import org.gbif.content.conf.ContentWsConfiguration.Synchronization.JenkinsJob;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.apache.http.client.utils.URIBuilder;

/**
 * Utility class that wraps the connection and interaction against  a Jenkins job.
 */
public class JenkinsJobClient {

  private final Synchronization syncConfig;

  /**
   * @param syncConfig url to the Jenkins job
   */
  public JenkinsJobClient(Synchronization syncConfig) {
    this.syncConfig = syncConfig;
  }

  /**
   * Executes the Jenkins Job.
   */
  public Response execute(String environment) throws IOException {
    HttpURLConnection connection=  null;
    try {
      connection = (HttpURLConnection)buildJenkinsJobUrl(environment).openConnection();
      Response.Status jenkinsJobStatus = Response.Status.fromStatusCode(connection.getResponseCode());
      if(Response.Status.Family.INFORMATIONAL == jenkinsJobStatus.getFamily()
         || Response.Status.Family.SUCCESSFUL == jenkinsJobStatus.getFamily()) {
        return Response.status(Response.Status.ACCEPTED)
          .header(HttpHeaders.LOCATION, Optional
            .ofNullable(connection.getHeaderField(HttpHeaders.LOCATION)).orElse(""))
          .build();
      }
      return Response.status(jenkinsJobStatus).build();
    } catch (Exception ex){
      return Response.serverError().entity(ex).build();
    } finally {
      Optional.ofNullable(connection).ifPresent(HttpURLConnection::disconnect);
    }
  }

  /**
   * Creates a Url instance to the Jenkins job.
   */
  public URL buildJenkinsJobUrl(String environment) throws URISyntaxException, MalformedURLException {
    return new URIBuilder(syncConfig.getJenkinsJobUrl())
                .addParameter(JenkinsJob.TOKEN_PARAM, syncConfig.getToken())
                .addParameter(JenkinsJob.CMD_PARAM, syncConfig.getCommand())
                .addParameter(JenkinsJob.REPOSITORY_PARAM, syncConfig.getRepository())
                .addParameter(JenkinsJob.ENV_PARAM, environment).build().toURL();
  }

}
