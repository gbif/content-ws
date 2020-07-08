package org.gbif.content.resource;

import org.gbif.content.config.ContentWsConfigurationProperties;

import java.io.IOException;

import com.google.common.io.Resources;
import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

import static org.junit.Assert.assertNotNull;

/**
 * Test cases for the ContentWsConfiguration class.
 */
public class ContentWsConfigurationPropertiesTest {

  private static final String CONFIG_FILE = "config.yml";

  /**
   * Loads configuration from test file.
   */
  public static ContentWsConfigurationProperties getTestConfiguration() {
    try {
      Yaml yaml = new Yaml();
      return yaml.loadAs(Resources.getResource(CONFIG_FILE).openStream(), ContentWsConfigurationProperties.class);
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Validates that the configuration test file can be loaded.
   */
  @Test
  public void testContentWsConfiguration() {
    ContentWsConfigurationProperties testConfiguration = getTestConfiguration();
    assertNotNull(testConfiguration);
    assertNotNull(testConfiguration.getSynchronization().getToken());
  }
}
