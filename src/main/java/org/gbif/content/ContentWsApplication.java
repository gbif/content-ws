package org.gbif.content;

import org.gbif.content.conf.ContentWsConfiguration;
import org.gbif.content.health.SearchHealthCheck;
import org.gbif.content.resource.EventsResource;
import org.gbif.content.resource.JenkinsJobClient;
import org.gbif.content.resource.SyncAuthenticator;
import org.gbif.content.resource.SyncResource;
import org.gbif.discovery.lifecycle.DiscoveryLifeCycle;

import java.security.Principal;
import java.util.Map;
import java.util.stream.Collectors;

import io.dropwizard.Application;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.elasticsearch.client.Client;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;

/**
 * Dropwizard application that exposes GBIF RSS feed and iCal entries based on CMS elements.
 */
public class ContentWsApplication extends Application<ContentWsConfiguration> {

  /**
   * Builds the ElasticSearch clients used for the synchronization service.
   */
  private static Map<String,Client> buildEsClients(ContentWsConfiguration configuration, Client defaultClient) {
    return configuration.getSynchronization().getIndexes()
            .entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey,
                                      e -> e.getValue().equals(configuration.getElasticSearch())?
                                      defaultClient: e.getValue().buildEsClient()));
  }

  /**
   * Executes the application and initializes al web resources.
   */
  @Override
  public void run(ContentWsConfiguration configuration, Environment environment) throws Exception {
    //Can be discovered in zookeeper
    if (configuration.getService().isDiscoverable()) {
      environment.lifecycle().manage(new DiscoveryLifeCycle(configuration.getService()));
    }
    Client searchIndex = configuration.getElasticSearch().buildEsClient();
    Map<String,Client> esClients = buildEsClients(configuration, searchIndex);

    environment.jersey().register(SyncAuthenticator.buildAuthFilter(configuration.getSynchronization().getToken()));
    environment.jersey().register(RolesAllowedDynamicFeature.class);
    //If you want to use @Auth to inject a custom Principal type into your resource
    environment.jersey().register(new AuthValueFactoryProvider.Binder<>(Principal.class));
    environment.jersey().register(new EventsResource(searchIndex, configuration));
    JenkinsJobClient jenkinsJobClient = new JenkinsJobClient(configuration.getSynchronization());
    environment.jersey().register(new SyncResource(jenkinsJobClient, esClients));
    environment.healthChecks().register("search", new SearchHealthCheck(esClients));
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
