package org.gbif.content.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Configuration settings to synchronize Contentful data into ElasticSearch.
 */
@Component
@ConfigurationProperties(prefix = "content.synchronization")
public class SynchronizationConfigurationProperties {

  private String jenkinsJobUrl = "http://builds.gbif.org/job/run-content-crawler/buildWithParameters";

  private String token;

  private String command = "contentful-crawl";

  private String repository = "snapshots";

  private Map<String, ElasticSearchProperties> indexes;

  public  Map<String, ElasticSearchProperties> getIndexes() {
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
