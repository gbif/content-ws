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

import org.gbif.content.crawl.contentful.crawl.EsDocBuilder;
import org.gbif.content.crawl.contentful.crawl.VocabularyTerms;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

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

@RequestMapping(value = "content", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
public class ContentResource {

  private static final int LEVELS = 2;

  private static final String LOCALE_PARAM = "locale";

  private static final String ALL = "*";

  private static final String CONTENT_ALIAS = "content";

  private final RestHighLevelClient esClient;

  private final CDAClient cdaPreviewClient;

  private final VocabularyTerms vocabularyTerms;

  @Autowired
  public ContentResource(
      RestHighLevelClient esClient, CDAClient cdaPreviewClient, VocabularyTerms vocabularyTerms) {
    this.esClient = esClient;
    this.cdaPreviewClient = cdaPreviewClient;
    this.vocabularyTerms = vocabularyTerms;
  }

  /**
   * Gets the content element from Elasticsearch.
   */
  @GetMapping("{id}")
  public ResponseEntity<Map<String, Object>> getContent(@PathVariable("id") String id) throws IOException {
      SearchResponse searchResponse =
          esClient.search(
              new SearchRequest()
                  .indices(CONTENT_ALIAS)
                  .source(new SearchSourceBuilder().query(QueryBuilders.termQuery("id", id)).size(1)),
                          RequestOptions.DEFAULT);
      return ResponseEntity.of(searchResponse.getHits().getHits().length > 0?
                                 Optional.ofNullable(searchResponse.getHits().getHits()[0].getSourceAsMap()) : Optional.empty());
  }

  /**
   * Builds a content response using the Contentful preview API.
   */
  @GetMapping("{id}/preview")
  public ResponseEntity<Map<String, Object>> getContentPreview(@PathVariable("id") String id) {
    try {
      CDAEntry cdaEntry = cdaPreviewClient.fetch(CDAEntry.class).include(LEVELS).where(LOCALE_PARAM, ALL).one(id);
      return ResponseEntity.ok(new EsDocBuilder(cdaEntry, vocabularyTerms, o -> {
      }).toEsDoc());
    } catch (CDAResourceNotFoundException ex) {
      return ResponseEntity.of(Optional.empty());
    }
  }
}
