package org.gbif.content;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(
    exclude = {
        ElasticsearchAutoConfiguration.class
    })
@EnableConfigurationProperties
public class ContentWsApplication {

  public static void main(String[] args) {
    SpringApplication.run(ContentWsApplication.class, args);
  }
}
