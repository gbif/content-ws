package org.gbif.content.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Content Web Service configuration class.
 */
@Component
@ConfigurationProperties(prefix = "content")
public class ContentWsConfigurationProperties {

  private String esNewsIndex = "news";

  private String esEventsIndex = "event";

  private String esDataUseIndex = "datause";

  private String esProgrammeIndex = "programme";

  private String defaultLocale = "en-GB";

  private String gbifPortalUrl = "http://www.gbif.org/";

  private SynchronizationProperties synchronization;

  private ElasticSearchProperties elasticSearch;

  public String getEsNewsIndex() {
    return esNewsIndex;
  }

  public void setEsNewsIndex(String esNewsIndex) {
    this.esNewsIndex = esNewsIndex;
  }

  public String getEsEventsIndex() {
    return esEventsIndex;
  }

  public void setEsEventsIndex(String esEventsIndex) {
    this.esEventsIndex = esEventsIndex;
  }

  public String getEsDataUseIndex() {
    return esDataUseIndex;
  }

  public void setEsDataUseIndex(String esDataUseIndex) {
    this.esDataUseIndex = esDataUseIndex;
  }

  public String getEsProgrammeIndex() {
    return esProgrammeIndex;
  }

  public void setEsProgrammeIndex(String esProgrammeIndex) {
    this.esProgrammeIndex = esProgrammeIndex;
  }

  public String getDefaultLocale() {
    return defaultLocale;
  }

  public void setDefaultLocale(String defaultLocale) {
    this.defaultLocale = defaultLocale;
  }

  public SynchronizationProperties getSynchronization() {
    return synchronization;
  }

  public void setSynchronization(SynchronizationProperties synchronization) {
    this.synchronization = synchronization;
  }

  public ElasticSearchProperties getElasticSearch() {
    return elasticSearch;
  }

  public void setElasticSearch(ElasticSearchProperties elasticSearch) {
    this.elasticSearch = elasticSearch;
  }

  public String getGbifPortalUrl() {
    return gbifPortalUrl;
  }

  public void setGbifPortalUrl(String gbifPortalUrl) {
    this.gbifPortalUrl = gbifPortalUrl;
  }
}
