package org.gbif.content.config;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * Configuration specific to interfacing with elastic search.
 */
public class ElasticSearchProperties {

  private String host = "localhost";

  private int port = 9300;

  private String cluster = "content-cluster";

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

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
    ElasticSearchProperties that = (ElasticSearchProperties) o;
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
