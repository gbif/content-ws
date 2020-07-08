package org.gbif.content.utils;

import org.springframework.http.HttpStatus;

public class WebApplicationException extends RuntimeException {
  private static final long serialVersionUID = 11660101L;
  private final Integer status;

  public WebApplicationException(String message, Integer status) {
    super(message);
    this.status = status;
  }

  public WebApplicationException(String message, HttpStatus status) {
    this(message, status.value());
  }

  public Integer getStatus() {
    return this.status;
  }
}
