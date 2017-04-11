package org.gbif.content;

import org.gbif.content.conf.ContentWsConfiguration;
import org.gbif.content.resource.EventsResource;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

/**
 * Dropwizard application that exposes GBIF RSS feed and iCal entries based on CMS elements.
 */
public class ContentWsApplication extends Application<ContentWsConfiguration> {

  /**
   * Executes the application and initializes al web resources.
   */
  @Override
  public void run(ContentWsConfiguration configuration, Environment environment) throws Exception {
    environment.jersey().register(new EventsResource(configuration.getElasticSearch().buildEsClient(),
                                                     configuration));
  }

  /**
   * Performs initializations.
   */
  @Override
  public void initialize(Bootstrap<ContentWsConfiguration> bootstrap) {
    // NOP
  }

  /**
   * Application entry point.
   */
  public static void main(String[] args) throws Exception {
    new ContentWsApplication().run(args);
  }
}
