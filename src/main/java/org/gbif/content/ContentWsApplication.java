package org.gbif.content;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class ContentWsApplication {

  public static void main(String[] args) {
    SpringApplication.run(ContentWsApplication.class, args);
  }
}
