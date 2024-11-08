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
package org.gbif.content.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Class to capture the used pieces of information when WebHoox request is received.
 */
public class WebHookRequest {

  private static final Map<String, Topic> TOPICS =
      Arrays.stream(Topic.values()).collect(Collectors.toMap(Topic::getValue, Function.identity()));

  public static final String CONTENTFUL_TOPIC_HEADER = "X-Contentful-Topic";

  /**
   * Recognized topics in the Webhook request.
   */
  public enum Topic {
    EntryPublish("ContentManagement.Entry.publish"),
    EntryUnPublish("ContentManagement.Entry.unpublish"),
    EntryDelete("ContentManagement.Entry.delete"),
    AssetPublish("ContentManagement.Asset.publish"),
    AssetUnPublish("ContentManagement.Asset.unpublish"),
    AssetDelete("ContentManagement.Asset.delete");

    private final String value;

    Topic(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  // Jackson mapper
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private String type;

  private String contentTypeId;

  private String id;

  private Topic topic;

  private String env = "dev'";

  /**
   * Element type to be modified.
   */
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getContentTypeId() {
    return contentTypeId;
  }

  public void setContentTypeId(String contentTypeId) {
    this.contentTypeId = contentTypeId;
  }

  /**
   * Id of the modified element.
   */
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  /**
   * Topic/action triggered by the sync action.
   */
  public Topic getTopic() {
    return topic;
  }

  public void setTopic(Topic topic) {
    this.topic = topic;
  }

  /**
   * Environment, used to distinguish the elastic search idx to be used.
   */
  public String getEnv() {
    return env;
  }

  public void setEnv(String env) {
    this.env = env;
  }

  /**
   * This method read the WebHook data from a servlet request.
   * The interpreted pieces of data are taken from the JSON fragment:
   * ...
   * "sys": {
   *    "type": "Entry",
   *    "contentType": {
   *      "sys": {
   *         "type": "Link",
   *         "linkType": "ContentType",
   *         "id": "DataUse"
   *       }
   *    },
   *    "id": "82531",
   *    ....
   * }
   */
  public static WebHookRequest fromRequest(HttpServletRequest request) {
    try {
      WebHookRequest webHookRequest = new WebHookRequest();
      Optional.ofNullable(request.getHeader(CONTENTFUL_TOPIC_HEADER))
          .map(TOPICS::get)
          .ifPresent(webHookRequest::setTopic);
      JsonNode jsonWebHook = MAPPER.readTree(request.getInputStream());
      webHookRequest.setType(jsonWebHook.at("/sys/type").asText());
      webHookRequest.setId(jsonWebHook.at("/sys/id").asText());
      webHookRequest.setContentTypeId(jsonWebHook.at("/sys/contentType/sys/id").asText());
      webHookRequest.setEnv(Optional.ofNullable(request.getParameter("env")).orElse("dev"));
      return webHookRequest;
    } catch (IOException ex) {
      throw new IllegalArgumentException(ex);
    }
  }
}
