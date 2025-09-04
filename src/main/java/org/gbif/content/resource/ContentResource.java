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
package org.gbif.content.resource;

import org.gbif.content.crawl.conf.ContentCrawlConfiguration;
import org.gbif.content.crawl.contentful.crawl.EsDocBuilder;
import org.gbif.content.crawl.contentful.crawl.VocabularyTerms;
import org.gbif.content.crawl.es.ElasticSearchUtils;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.contentful.java.cda.CDAClient;
import com.contentful.java.cda.CDAEntry;
import com.contentful.java.cda.CDAResourceNotFoundException;
import com.contentful.java.cma.CMAClient;
import com.contentful.java.cma.model.CMAContentType;

import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.SneakyThrows;

@RequestMapping(value = "content", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
public class ContentResource {

  private static final int LEVELS = 2;

  private static final String LOCALE_PARAM = "locale";

  private static final String ALL = "*";

  private static final String CONTENT_ALIAS = "content";

  private final RestHighLevelClient esClient;

  private final CDAClient cdaPreviewClient;

  private final CMAClient cmaClient;

  private final VocabularyTerms vocabularyTerms;

  private final ContentCrawlConfiguration.Contentful configuration;

  private String projectContentId;

  private final Set<String> tagFields;

  @Autowired
  public ContentResource(
      RestHighLevelClient esClient,
      CDAClient cdaPreviewClient,
      VocabularyTerms vocabularyTerms,
      ContentCrawlConfiguration.Contentful configuration,
      CMAClient cmaClient) {
    this.esClient = esClient;
    this.cdaPreviewClient = cdaPreviewClient;
    this.vocabularyTerms = vocabularyTerms;
    this.configuration = configuration;
    this.cmaClient = cmaClient;
    this.tagFields =
        configuration.getContentTypes().stream()
            .map(contentType -> ElasticSearchUtils.toFieldNameFormat(contentType) + "Tag")
            .collect(Collectors.toSet());
  }

  private String getProjectContentId() {
    if (projectContentId == null) {
      this.projectContentId = lookUpProjectContentId();
    }
    return projectContentId;
  }

  private String lookUpProjectContentId() {
    return cmaClient.contentTypes().fetchAll().getItems().stream()
        .filter(
            cmaContentType ->
                cmaContentType.getName().equalsIgnoreCase(configuration.getProjectContentType()))
        .findFirst()
        .map(CMAContentType::getId)
        .orElseThrow(() -> new RuntimeException("Project Content Type not Found"));
  }

  /**
   * Gets the content element from Elasticsearch.
   */
  @GetMapping("{id}")
  public ResponseEntity<Map<String, Object>> getContent(@PathVariable("id") String id) {
    return ResponseEntity.of(getEsDoc(id));
  }

  @SneakyThrows
  private Optional<Map<String, Object>> getEsDoc(String id) {
    SearchResponse searchResponse =
        esClient.search(
            new SearchRequest()
                .indices(CONTENT_ALIAS)
                .source(new SearchSourceBuilder().query(QueryBuilders.termQuery("id", id)).size(1)),
            RequestOptions.DEFAULT);
    return searchResponse.getHits().getHits().length > 0
        ? Optional.ofNullable(searchResponse.getHits().getHits()[0].getSourceAsMap())
        : Optional.empty();
  }

  /**
   * Get the tags fields of a es document.
   */
  private Map<String, Object> getTagFields(Map<String, Object> sourceMap) {
    return sourceMap.entrySet().stream()
        .filter(entry -> tagFields.contains(entry.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * Builds a content response using the Contentful preview API.
   */
  @GetMapping("{id}/preview")
  public ResponseEntity<Map<String, Object>> getContentPreview(@PathVariable("id") String id) {
    try {
      CDAEntry cdaEntry = fetchEntry(cdaPreviewClient, id, LEVELS, LOCALE_PARAM, ALL);
      Map<String, Object> esDoc =
          new EsDocBuilder(cdaEntry, vocabularyTerms, getProjectContentId(), o -> {}).toEsDoc();
      getEsDoc(id).map(this::getTagFields).ifPresent(esDoc::putAll);
      return ResponseEntity.ok(esDoc);
    } catch (CDAResourceNotFoundException ex) {
      return ResponseEntity.of(Optional.empty());
    }
  }

  @RateLimiter(name = "contentfulApi")
  @Retry(name = "contentfulApiRetry")
  public CDAEntry fetchEntry(
      CDAClient cdaPreviewClient, String id, int levels, String localeParam, String all) {
    return cdaPreviewClient.fetch(CDAEntry.class).include(levels).where(localeParam, all).one(id);
  }
}
