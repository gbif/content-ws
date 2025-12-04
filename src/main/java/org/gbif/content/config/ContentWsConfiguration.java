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

import org.gbif.content.crawl.conf.ContentCrawlConfiguration;
import org.gbif.content.crawl.contentful.crawl.VocabularyTerms;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.contentful.java.cda.CDAClient;
import com.contentful.java.cma.CMAClient;

@Configuration
public class ContentWsConfiguration {

  // 3 Minutes
  private static final int CONNECTION_TO = 3;

  @Bean
  public RestClient elasticsearchRestClient(ContentWsProperties properties) {
    return buildRestClient(properties.getElasticsearch());
  }

  @Bean
  public ElasticsearchClient searchClient(RestClient elasticsearchRestClient) {
    ElasticsearchTransport transport = new RestClientTransport(elasticsearchRestClient, new JacksonJsonpMapper());
    return new ElasticsearchClient(transport);
  }

  @ConfigurationProperties(prefix = "content")
  @Bean
  public ContentWsProperties contentWsProperties() {
    return new ContentWsProperties();
  }

  public static RestClient buildRestClient(ElasticsearchProperties properties) {
    try {
      List<HttpHost> httpHosts = new ArrayList<>();
      for (String hostUrl : properties.getHosts()) {
        URL urlHost = new URL(hostUrl);
        HttpHost host = new HttpHost(urlHost.getHost(), urlHost.getPort(), urlHost.getProtocol());
        httpHosts.add(host);
      }
      
      return RestClient.builder(httpHosts.toArray(new HttpHost[0]))
          .setRequestConfigCallback(
              requestConfigBuilder ->
                  requestConfigBuilder
                      .setConnectTimeout(properties.getConnectionTimeOut())
                      .setSocketTimeout(properties.getSocketTimeOut())
                      .setConnectionRequestTimeout(properties.getConnectionRequestTimeOut()))
          .build();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static ElasticsearchClient searchClient(ElasticsearchProperties properties) {
    RestClient restClient = buildRestClient(properties);
    ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
    return new ElasticsearchClient(transport);
  }

  @ConfigurationProperties(prefix = "contentful")
  @Bean
  public ContentCrawlConfiguration.Contentful contentfulProperties() {
    return new ContentCrawlConfiguration.Contentful();
  }

  /**
   * @return a new instance of a Contentful CMAClient.
   */
  @Bean
  public CMAClient buildCmaClient(ContentCrawlConfiguration.Contentful configuration) {
    return new CMAClient.Builder()
        .setSpaceId(configuration.getSpaceId())
        .setEnvironmentId(configuration.getEnvironmentId())
        .setAccessToken(configuration.getCmaToken())
        .build();
  }

  /**
   * @return a new instance of a Contentful Preview CDAClient.
   */
  @Bean
  public CDAClient cadPreviewClient(ContentCrawlConfiguration.Contentful configuration) {
    CDAClient.Builder builder = CDAClient.builder();
    return builder
        .setSpace(configuration.getSpaceId())
        .setToken(configuration.getCdaToken())
        .setEnvironment(configuration.getEnvironmentId())
        .preview()
        .setCallFactory(
            builder
                .defaultCallFactoryBuilder()
                .readTimeout(CONNECTION_TO, TimeUnit.MINUTES)
                .retryOnConnectionFailure(true)
                .build())
        .build();
  }

  @Bean
  public VocabularyTerms vocabularyTerms(
      ContentCrawlConfiguration.Contentful configuration,
      CMAClient cmaClient,
      @Value("${contentful.preloadVocabularies:true}") boolean preLoad) {
    VocabularyTerms vocabularyTerms = new VocabularyTerms();
    if (preLoad) {
      cmaClient
          .contentTypes()
          .fetchAll(configuration.getSpaceId(), configuration.getEnvironmentId())
          .getItems()
          .forEach(
              contentType -> {
                if (configuration.getVocabularies().contains(contentType.getName())) {
                  // Keeps the country vocabulary ID for future use
                  if (contentType.getName().equals(configuration.getCountryVocabulary())) {
                    vocabularyTerms.loadCountryVocabulary(contentType);
                  } else {
                    // Loads vocabulary into memory
                    vocabularyTerms.loadVocabulary(contentType);
                  }
                }
              });
    }
    return vocabularyTerms;
  }
}
