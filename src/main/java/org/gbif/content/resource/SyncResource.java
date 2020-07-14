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
package org.gbif.content.resource;

import org.gbif.content.config.ContentWsProperties;
import org.gbif.content.service.JenkinsJobClient;
import org.gbif.content.service.WebHookRequest;
import org.gbif.content.service.WebHookRequest.Topic;
import org.gbif.content.utils.Paths;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codahale.metrics.annotation.Timed;

/**
 * Resource class that provides RSS and iCal feeds for events and news.
 */
@RestController
@RequestMapping(path = Paths.SYNC_RESOURCE_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class SyncResource {

  private static final Logger LOG = LoggerFactory.getLogger(SyncResource.class);

  public static final String CONTENTFUL_CONTENT_TYPE =
      "application/vnd.contentful.management.v1+json";
  // Used to map indices names
  private static final Pattern REPLACEMENTS = Pattern.compile(":\\s+|\\s+");
  private static final String ES_TYPE = "content";

  private final JenkinsJobClient jenkinsJobClient;
  private final Map<String, Client> esClients;

  /**
   * Full constructor: requires the configuration object and an ElasticSearch client.
   */
  public SyncResource(
      JenkinsJobClient jenkinsJobClient, Client searchIndex, ContentWsProperties properties) {
    this.jenkinsJobClient = jenkinsJobClient;
    this.esClients = buildEsClients(properties, searchIndex);
  }

  /**
   * Builds the ElasticSearch clients used for the synchronization service.
   * The default client if the same ElasticSearch server is configured as a sync index
   */
  private static Map<String, Client> buildEsClients(
      ContentWsProperties properties, Client defaultClient) {
    return properties.getSynchronization().getIndexes().entrySet().stream()
        .collect(
            Collectors.toMap(
                Map.Entry::getKey,
                e ->
                    e.getValue().equals(properties.getElasticSearch())
                        ? defaultClient
                        : e.getValue().buildEsClient()));
  }

  /**
   * Synchronization WebHook.
   * This service listens notification from Contentful WebHooks to syncronize the content of ElasticSearch indices.
   */
  @Timed
  @PostMapping(consumes = CONTENTFUL_CONTENT_TYPE)
  public ResponseEntity<?> sync(HttpServletRequest request) {
    WebHookRequest webHookRequest = WebHookRequest.fromRequest(request);
    return Optional.ofNullable(webHookRequest.getTopic())
        .map(
            topic -> {
              LOG.info("Action received {}", topic);
              // Only deletions are handled
              if (Topic.EntryUnPublish == topic || Topic.EntryDelete == topic) {
                return deleteDocument(webHookRequest);
              }
              // Rest of recognised topics/commands trigger a full crawl
              return runFullCrawl(webHookRequest.getEnv());
            })
        .orElseGet(
            () -> {
              LOG.warn(
                  "Unsupported operation {}",
                  request.getHeader(WebHookRequest.CONTENTFUL_TOPIC_HEADER));
              return ResponseEntity.badRequest().build();
            });
  }

  /**
   * Deletes a document from ElasticSearch.
   */
  private ResponseEntity<?> deleteDocument(WebHookRequest webHookRequest) {
    DeleteResponse deleteResponse =
        esClients
            .get(webHookRequest.getEnv())
            .prepareDelete(
                getEsIdxName(webHookRequest.getContentTypeId()), ES_TYPE, webHookRequest.getId())
            .get();
    LOG.info("Entry deleted");

    return ResponseEntity.status(deleteResponse.status().getStatus()).build();
  }

  /**
   * Gets the idx name from a content type name.
   */
  private static String getEsIdxName(String contentTypeName) {
    return REPLACEMENTS.matcher(contentTypeName).replaceAll("").toLowerCase();
  }

  /**
   * Sends a full crawl request to the Jenkins sync job.
   */
  private ResponseEntity<?> runFullCrawl(String environment) {
    return jenkinsJobClient.execute(environment);
  }
}
