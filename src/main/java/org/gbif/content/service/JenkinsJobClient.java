package org.gbif.content.service;

import org.apache.http.client.utils.URIBuilder;
import org.gbif.content.config.JenkinsJob;
import org.gbif.content.config.SynchronizationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

/**
 * Utility class that wraps the connection and interaction against  a Jenkins job.
 */
public class JenkinsJobClient {

  private final SynchronizationProperties syncConfig;

  /**
   * @param syncConfig url to the Jenkins job
   */
  public JenkinsJobClient(SynchronizationProperties syncConfig) {
    this.syncConfig = syncConfig;
  }

  /**
   * Executes the Jenkins Job.
   */
  public ResponseEntity<?> execute(String environment) {
    HttpURLConnection connection = null;
    try {
      connection = (HttpURLConnection)buildJenkinsJobUrl(environment).openConnection();
      HttpStatus jenkinsJobStatus = HttpStatus.resolve(connection.getResponseCode());
      if (jenkinsJobStatus != null && (jenkinsJobStatus.is1xxInformational() || jenkinsJobStatus.is2xxSuccessful())) {
        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .header(HttpHeaders.LOCATION,
                Optional.ofNullable(connection.getHeaderField(HttpHeaders.LOCATION)).orElse(""))
          .build();
      }
      return ResponseEntity.status(jenkinsJobStatus).build();
    } catch (Exception ex) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex);
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
