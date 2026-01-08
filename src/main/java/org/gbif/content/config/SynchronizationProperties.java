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
package org.gbif.content.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

/**
 * Configuration settings to synchronize Contentful data into ElasticSearch.
 */
@Component
@ConfigurationProperties(prefix = "content.synchronization")
@Data
public class SynchronizationProperties {

  /**
   * URL to the Jenkins job that runs a full syncronization.
   */
  private String jenkinsJobUrl =
    "https://builds.gbif.org/job/run-content-crawler/buildWithParameters";

  /**
   * Jenkins job security token.
   */
  private String token;

  /**
   * Command parameter of the Jenkins sync job.
   */
  private String command = "contentful-crawl";

  /**
   * Classifier parameter of the Jenkins sync job.
   */
  private String classifier = "";

  /**
   * Version parameter of the Jenkins sync job.
   */
  private String version = "latest";

  private Map<String, EnvironmentConfig> environments;

  @Data
  public static class EnvironmentConfig {

    /**
     * Command parameter to set the Nexus repository.
     */
    private String repository = "snapshots";

    private ElasticsearchProperties index;

  }
}
