package org.gbif.content.conf;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

public class ContentWsConfiguration extends Configuration {

  /**
   * Configuration specific to interfacing with elastic search.
   */
  public static class ElasticSearch {

    public String host = "localhost";

    public int port = 9300;

    public String cluster = "content-cluster";

    @JsonProperty
    public String getHost() {
      return host;
    }

    public void setHost(String host) {
      this.host = host;
    }

    @JsonProperty
    public int getPort() {
      return port;
    }

    public void setPort(int port) {
      this.port = port;
    }

    @JsonProperty
    public String getCluster() {
      return cluster;
    }

    public void setCluster(String cluster) {
      this.cluster = cluster;
    }


    /**
     * Creates a new instance of a ElasticSearch client.
     */
    public Client buildEsClient() {
      try {
        Settings settings = Settings.builder().put("cluster.name", cluster).build();
        return new PreBuiltTransportClient(settings).addTransportAddress(
          new InetSocketTransportAddress(InetAddress.getByName(host), port));
      } catch (UnknownHostException ex) {
        throw new IllegalStateException(ex);
      }
    }
  }

  private ElasticSearch elasticSearch = new ElasticSearch();

  private String esNewsIndex = "news";

  private String esEventsIndex = "event";

  private String esDataUseIndex = "datause";

  @JsonProperty
  public ElasticSearch getElasticSearch() {
    return elasticSearch;
  }

  public void setElasticSearch(ElasticSearch elasticSearch) {
    this.elasticSearch = elasticSearch;
  }

  @JsonProperty
  public String getEsNewsIndex() {
    return esNewsIndex;
  }

  public void setEsNewsIndex(String esNewsIndex) {
    this.esNewsIndex = esNewsIndex;
  }

  @JsonProperty
  public String getEsEventsIndex() {
    return esEventsIndex;
  }

  public void setEsEventsIndex(String esEventsIndex) {
    this.esEventsIndex = esEventsIndex;
  }

  @JsonProperty
  public String getEsDataUseIndex() {
    return esDataUseIndex;
  }

  public void setEsDataUseIndex(String esDataUseIndex) {
    this.esDataUseIndex = esDataUseIndex;
  }


}
