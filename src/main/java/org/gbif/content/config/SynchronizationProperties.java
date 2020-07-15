/*
 * Copyright 2020 Global Biodiversity Information Facility (GBIF)
 *
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

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration settings to synchronize Contentful data into ElasticSearch.
 */
@Component
@ConfigurationProperties(prefix = "content.synchronization")
public class SynchronizationProperties {

  private String jenkinsJobUrl =
      "https://builds.gbif.org/job/run-content-crawler/buildWithParameters";

  private String token;

  private String command = "contentful-crawl";

  private String repository = "snapshots";

  private Map<String, ElasticSearchProperties> indexes;

  public Map<String, ElasticSearchProperties> getIndexes() {
    return indexes;
  }

  public void setIndexes(Map<String, ElasticSearchProperties> indexes) {
    this.indexes = indexes;
  }

  public ElasticSearchProperties getIndex(String env) {
    return indexes.get(env);
  }

  /**
   * URL to the Jenkins job that runs a full syncronization.
   */
  public String getJenkinsJobUrl() {
    return jenkinsJobUrl;
  }

  public void setJenkinsJobUrl(String jenkinsJobUrl) {
    this.jenkinsJobUrl = jenkinsJobUrl;
  }

  /**
   * Jenkins job security token.
   */
  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }

  /**
   * Command parameter of the Jenkins sync job.
   */
  public String getCommand() {
    return command;
  }

  public void setCommand(String command) {
    this.command = command;
  }

  /**
   * Command parameter to set the Nexus repository.
   */
  public String getRepository() {
    return repository;
  }

  public void setRepository(String repository) {
    this.repository = repository;
  }
}
