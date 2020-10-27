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

import java.net.URL;

import org.apache.http.HttpHost;
import org.elasticsearch.client.NodeSelector;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EsConfiguration {

  @Bean
  public RestHighLevelClient searchClient(ContentWsProperties properties) {
    return searchClient(properties.getElasticsearch());
  }

  @ConfigurationProperties(prefix = "content")
  @Bean
  public ContentWsProperties contentWsProperties() {
    return new ContentWsProperties();
  }

  public static RestHighLevelClient searchClient(ElasticsearchProperties properties) {
    try {
      URL urlHost = new URL(properties.getHost());
      HttpHost host = new HttpHost(urlHost.getHost(), urlHost.getPort(), urlHost.getProtocol());
      return new RestHighLevelClient(RestClient.builder(host)
                                       .setRequestConfigCallback(requestConfigBuilder ->
                                                                   requestConfigBuilder
                                                                     .setConnectTimeout(properties.getConnectionTimeOut())
                                                                     .setSocketTimeout(properties.getSocketTimeOut())
                                                                     .setConnectionRequestTimeout(properties.getConnectionRequestTimeOut()))
                                       .setNodeSelector(NodeSelector.SKIP_DEDICATED_MASTERS));
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }
}
