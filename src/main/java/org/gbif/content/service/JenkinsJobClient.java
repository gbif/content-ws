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
package org.gbif.content.service;

import org.gbif.content.config.SynchronizationProperties;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;

import org.apache.http.client.utils.URIBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Utility class that wraps the connection and interaction against  a Jenkins job.
 */
@Component
public class JenkinsJobClient {

  public static final String TOKEN_PARAM = "token";
  public static final String ENV_PARAM = "environment";
  public static final String CMD_PARAM = "command";
  public static final String REPOSITORY_PARAM = "repository";

  private final SynchronizationProperties syncProperties;

  /**
   * @param syncProperties url to the Jenkins job
   */
  public JenkinsJobClient(SynchronizationProperties syncProperties) {
    this.syncProperties = syncProperties;
  }

  /**
   * Executes the Jenkins Job.
   */
  public ResponseEntity<?> execute(String environment) {
    HttpURLConnection connection = null;
    try {
      connection = (HttpURLConnection) buildJenkinsJobUrl(environment).openConnection();
      HttpStatus jenkinsJobStatus = HttpStatus.resolve(connection.getResponseCode());
      if (jenkinsJobStatus != null
          && (jenkinsJobStatus.is1xxInformational() || jenkinsJobStatus.is2xxSuccessful())) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
            .header(
                HttpHeaders.LOCATION,
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
  public URL buildJenkinsJobUrl(String environment)
      throws URISyntaxException, MalformedURLException {
    SynchronizationProperties.EnvironmentConfig envConfig = syncProperties.getEnvironments().get(environment);
        syncProperties.getEnvironments().get(environment);
    return new URIBuilder(syncProperties.getJenkinsJobUrl())
        .addParameter(TOKEN_PARAM, syncProperties.getToken())
        .addParameter(CMD_PARAM, syncProperties.getCommand())
        .addParameter(REPOSITORY_PARAM, envConfig.getRepository())
        .addParameter(ENV_PARAM, environment)
        .build()
        .toURL();
  }
}
