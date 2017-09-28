package org.gbif.content.conf;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import org.gbif.discovery.conf.ServiceConfiguration;

/**
 * Content Web Service configuration class.
 */
public class ContentWsConfiguration extends Configuration {

  /**
   * Configuration specific to interfacing with elastic search.
   */
  public static class ElasticSearch {

    private String host = "localhost";

    private int port = 9300;

    private String cluster = "content-cluster";

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

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      ElasticSearch that = (ElasticSearch) o;
      return port == that.port
             && Objects.equals(host, that.host)
             && Objects.equals(cluster, that.cluster);
    }

    @Override
    public int hashCode() {
      return Objects.hash(host, port, cluster);
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

  /**
   * Configuration settings to synchronize Contentful data into ElasticSearch.
   */
  public static class Synchronization {

    /**
     * Jenkins Job parameters.
     */
    public static class JenkinsJob {
      public static final String TOKEN_PARAM = "token";
      public static final String ENV_PARAM = "environment";
      public static final String CMD_PARAM = "command";
      public static final String REPOSITORY_PARAM = "repository";

      /**
       * Private constructor.
       */
      private JenkinsJob() {
        //do nothing
      }
    }

    private String jenkinsJobUrl = "http://builds.gbif.org/job/run-content-crawler/buildWithParameters";

    private String token;

    private String command = "contentful-crawl";

    private String repository = "snapshots";

    private Map<String, ElasticSearch> indexes;

    @JsonProperty
    public  Map<String, ElasticSearch> getIndexes() {
      return indexes;
    }

    public void setIndexes(Map<String, ElasticSearch> indexes) {
      this.indexes = indexes;
    }

    public ElasticSearch getIndex(String env) {
      return indexes.get(env);
    }

    /**
     * URL to the Jenkins job that runs a full syncronization.
     */
    @JsonProperty
    public String getJenkinsJobUrl() {
      return jenkinsJobUrl;
    }

    public void setJenkinsJobUrl(String jenkinsJobUrl) {
      this.jenkinsJobUrl = jenkinsJobUrl;
    }

    /**
     * Jenkins job security token.
     */
    @JsonProperty
    public String getToken() {
      return token;
    }

    public void setToken(String token) {
      this.token = token;
    }

    /**
     * Command parameter of the Jenkins sync job.
     */
    @JsonProperty
    public String getCommand() {
      return command;
    }

    public void setCommand(String command) {
      this.command = command;
    }

    /**
     * Command parameter to set the Nexus repository.
     */
    @JsonProperty
    public String getRepository() {
      return repository;
    }

    public void setRepository(String repository) {
      this.repository = repository;
    }

  }

  private String esNewsIndex = "news";

  private String esEventsIndex = "event";

  private String esDataUseIndex = "datause";

  private String esProgrammeIndex = "programme";

  private String defaultLocale = "en-GB";

  private Synchronization synchronization;

  private ServiceConfiguration service;

  private ElasticSearch elasticSearch;


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

  @JsonProperty
  public String getEsProgrammeIndex() {
    return esProgrammeIndex;
  }

  public void setEsProgrammeIndex(String esProgrammeIndex) {
    this.esProgrammeIndex = esProgrammeIndex;
  }

  @JsonProperty
  public String getDefaultLocale() {
    return defaultLocale;
  }

  public void setDefaultLocale(String defaultLocale) {
    this.defaultLocale = defaultLocale;
  }

  @JsonProperty
  public Synchronization getSynchronization() {
    return synchronization;
  }

  public void setSynchronization(Synchronization synchronization) {
    this.synchronization = synchronization;
  }

  @JsonProperty
  public ServiceConfiguration getService() {
    return service;
  }

  public void setService(ServiceConfiguration service) {
    this.service = service;
  }

  @JsonProperty
  public ElasticSearch getElasticSearch() {
    return elasticSearch;
  }

  public void setElasticSearch(ElasticSearch elasticSearch) {
    this.elasticSearch = elasticSearch;
  }
}
