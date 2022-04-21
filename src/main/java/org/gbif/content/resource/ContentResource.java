package org.gbif.content.resource;

import org.gbif.content.crawl.contentful.crawl.EsDocBuilder;
import org.gbif.content.crawl.contentful.crawl.VocabularyTerms;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import com.contentful.java.cda.CDAClient;

import com.contentful.java.cda.CDAEntry;

import com.contentful.java.cda.CDAResourceNotFoundException;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping(value = "content", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
public class ContentResource {

  private static final int LEVELS = 2;

  private static final String LOCALE_PARAM = "locale";

  private static final String ALL = "*";

  private final RestHighLevelClient esClient;

  private final CDAClient cdaPreviewClient;

  private final VocabularyTerms vocabularyTerms;

  @Autowired
  public ContentResource(RestHighLevelClient esClient, CDAClient cdaPreviewClient, VocabularyTerms vocabularyTerms) {
    this.esClient = esClient;
    this.cdaPreviewClient = cdaPreviewClient;
    this.vocabularyTerms = vocabularyTerms;
  }

  @GetMapping("{id}")
  public ResponseEntity<Map<String,Object>> getContent(@PathVariable("id") String id, @RequestParam(value = "preview", defaultValue = "false") boolean preview) throws
    IOException {
      if (preview) {
        try {
          CDAEntry cdaEntry = cdaPreviewClient.fetch(CDAEntry.class).include(LEVELS).where(LOCALE_PARAM, ALL).one(id);
          return ResponseEntity.ok(new EsDocBuilder(cdaEntry, vocabularyTerms, o -> {}).toEsDoc());
        } catch (CDAResourceNotFoundException ex) {
          return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyMap());
        }
      } else {
        SearchResponse searchResponse = esClient.search(new SearchRequest().indices("content").source(new SearchSourceBuilder().query(QueryBuilders.termQuery("id", id)).size(1)), RequestOptions.DEFAULT);
        return searchResponse.getHits().getHits().length > 0? ResponseEntity.ok(searchResponse.getHits().getHits()[0].getSourceAsMap()) : ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyMap());
      }
  }
}
