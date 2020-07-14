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
package org.gbif.content.health;

import java.util.Map;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;

import com.codahale.metrics.health.HealthCheck;

/**
 * Checks that the ElasticSearch cluster is healthy and responding.
 */
public class SearchHealthCheck extends HealthCheck {

  // ElasticSearch alias for all content indices
  private static final String CONTENT_IDX = "content";

  // ElasticSearch client
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
  protected Result check() {
    if (esClients == null) {
      return Result.unhealthy("ElasticSearch has not being initialized");
    }
    esClients
        .values()
        .forEach(
            esClient -> {
              SearchResponse response =
                  esClient
                      .prepareSearch(CONTENT_IDX)
                      .setQuery(QueryBuilders.matchAllQuery())
                      .setSize(0)
                      .get();
              if (response.getFailedShards() > 0) {
                Result.unhealthy(
                    "Some shards reported errors performing a search all operation %s",
                    response.getShardFailures());
              }
            });
    return Result.healthy();
  }
}
