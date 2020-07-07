package org.gbif.content.config;

/**
 * Jenkins Job parameters.
 */
public class JenkinsJob {
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
