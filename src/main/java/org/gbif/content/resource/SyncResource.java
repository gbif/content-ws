package org.gbif.content.resource;


import org.gbif.content.conf.ContentWsConfiguration;
import org.gbif.content.resource.WebHookRequest.Topic;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.codahale.metrics.annotation.Timed;
import org.apache.http.client.utils.URIBuilder;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.client.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Resource class that provides RSS and iCal feeds for events and news.
 */
@Path("/content/sync")
@Produces(MediaType.APPLICATION_JSON)
public class SyncResource {

  /**
   * Jenkins Job parameters.
   */
  public static class JenkinsJob {
    public static final String TOKEN_PARAM = "token";
    public static final String ENV_PARAM = "environment";
    public static final String CMD_PARAM = "command";
  }

  private static final Logger LOG = LoggerFactory.getLogger(SyncResource.class);

  private static final String CONTENTFUL_CONTENT_TYPE = "application/vnd.contentful.management.v1+json";

  private static final String LOCATION_HEADER = "Location";

  //Used to map indices names
  private static final Pattern REPLACEMENTS = Pattern.compile(":\\s+|\\s+");

  private static final String ES_TYPE = "content";

  private final URL jenkinsJobUrl;

  private final Client esClient;

  /**
   * Full constructor: requires the configuration object and an ElasticSearch client.
   */
  public SyncResource(ContentWsConfiguration.Synchronization configuration, Client esClient) {
    try {
      //Builds a URL to the Jenkins sync job
      URIBuilder builder = new URIBuilder(configuration.getJenkinsJobUrl());
      builder.addParameter(JenkinsJob.TOKEN_PARAM, configuration.getToken());
      builder.addParameter(JenkinsJob.ENV_PARAM, configuration.getEnvironment());
      builder.addParameter(JenkinsJob.CMD_PARAM, configuration.getCommand());
      jenkinsJobUrl = builder.build().toURL();
      this.esClient = esClient;
    } catch (MalformedURLException | URISyntaxException ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Synchronization WebHook.
   * This service listens notification from Contentful WebHooks to syncronize the content of ElasticSearch indices.
   */
  @POST
  @Timed
  @Consumes(CONTENTFUL_CONTENT_TYPE)
  public Response sync(@Context HttpServletRequest request) {
    WebHookRequest webHookRequest = WebHookRequest.fromRequest(request);
    return Optional.ofNullable(webHookRequest.getTopic())
            .map(topic -> {
              LOG.info("Action received {}", topic);
              //Only deletions are handled
              if (Topic.EntryUnPublish == topic || Topic.EntryDelete == topic) {
                return deleteDocument(webHookRequest);
              }
              //Rest of recognised topics/commands trigger a full crawl
              return runFullCrawl();
            }).orElseGet(() -> {
              LOG.warn("Unsupported operation {}", request.getHeader(WebHookRequest.CONTENTFUL_TOPIC_HEADER));
              return Response.status(Response.Status.BAD_REQUEST).build();
            });
  }

  /**
   * Deletes a document from ElasticSearch.
   */
  private Response deleteDocument(WebHookRequest webHookRequest) {
    DeleteResponse deleteResponse = esClient.prepareDelete(getEsIdxName(webHookRequest.getContentTypeId()),
                                                           ES_TYPE, webHookRequest.getId()).get();
    LOG.info("Entry deleted");
    return Response.status(deleteResponse.status().getStatus()).build();
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
  private Response runFullCrawl() {
    HttpURLConnection connection=  null;
    try {
      connection = (HttpURLConnection)jenkinsJobUrl.openConnection();
      Response.Status jenkinsJobStatus = Response.Status.fromStatusCode(connection.getResponseCode());
      if(Response.Status.Family.INFORMATIONAL == jenkinsJobStatus.getFamily()
         || Response.Status.Family.SUCCESSFUL == jenkinsJobStatus.getFamily()) {
        return Response.status(Response.Status.ACCEPTED)
                .header(LOCATION_HEADER, Optional.ofNullable(connection.getHeaderField(LOCATION_HEADER)).orElse(""))
                .build();
      }
      return Response.status(jenkinsJobStatus).build();
    } catch (IOException ex) {
      LOG.error("Error sending request to Jenkins", ex);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    } finally {
      Optional.ofNullable(connection).ifPresent(HttpURLConnection::disconnect);
    }
  }
}
