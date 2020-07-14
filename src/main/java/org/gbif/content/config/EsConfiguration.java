package org.gbif.content.config;

import org.elasticsearch.client.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EsConfiguration {

  private final ContentWsProperties properties;

  public EsConfiguration(ContentWsProperties properties) {
    this.properties = properties;
  }

  @Bean
  public Client searchClient() {
    return properties.getElasticSearch().buildEsClient();
  }
}
