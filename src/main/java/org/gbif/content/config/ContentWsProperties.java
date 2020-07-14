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

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Content Web Service configuration class.
 */
@Component
@ConfigurationProperties(prefix = "content")
public class ContentWsProperties {

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
