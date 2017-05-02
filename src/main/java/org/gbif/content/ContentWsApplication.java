package org.gbif.content;

import org.gbif.content.conf.ContentWsConfiguration;
import org.gbif.content.resource.EventsResource;
import org.gbif.content.resource.SyncResource;
import org.gbif.discovery.lifecycle.DiscoveryLifeCycle;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.elasticsearch.client.Client;

/**
 * Dropwizard application that exposes GBIF RSS feed and iCal entries based on CMS elements.
 */
public class ContentWsApplication extends Application<ContentWsConfiguration> {

  /**
   * Executes the application and initializes al web resources.
   */
  @Override
  public void run(ContentWsConfiguration configuration, Environment environment) throws Exception {
    //Can be discovered in zookeeper
    if (configuration.getService().isDiscoverable()) {
      environment.lifecycle().manage(new DiscoveryLifeCycle(configuration.getService()));
    }
    Client esClient = configuration.getElasticSearch().buildEsClient();
    environment.jersey().register(new EventsResource(esClient, configuration));
    environment.jersey().register(new SyncResource(configuration.getSynchronization(), esClient));
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
