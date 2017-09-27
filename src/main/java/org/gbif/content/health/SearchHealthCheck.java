package org.gbif.content.health;

import java.util.Map;

import com.codahale.metrics.health.HealthCheck;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * Checks that the ElasticSearch cluster is healthy and responding.
 */
public class SearchHealthCheck extends HealthCheck {

  //ElasticSearch alias for all content indices
  private static final String CONTENT_IDX = "content";

  //ElasticSearch client
  private final Map<String, Client> esClients;

  /**
   *
   * @param esClients ElasticSearch client
   */
  public SearchHealthCheck(Map<String, Client> esClients) {
    this.esClients = esClients;
  }

  /**
   * Validates that the ElasticSearch client can perform queries against the content alias.
   */
  @Override
  protected Result check() throws Exception {
    if (esClients == null) {
      return Result.unhealthy("ElasticSearch has not being initialized");
    }
    esClients.values().forEach(esClient -> {
      SearchResponse response = esClient.prepareSearch(CONTENT_IDX)
        .setQuery(QueryBuilders.matchAllQuery()).setSize(0).get();
      if (response.getFailedShards() > 0) {
        Result.unhealthy("Some shards reported errors performing a search all operation %s",
                         response.getShardFailures());
      }
    });
    return Result.healthy();
  }
}
