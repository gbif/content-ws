package org.gbif.content.advice;

import org.gbif.content.exception.WebApplicationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class WebApplicationExceptionMapper {

  public WebApplicationExceptionMapper() {
  }

  @ExceptionHandler({WebApplicationException.class})
  public ResponseEntity<?> handleWebApplicationException(WebApplicationException e) {
    return ResponseEntity
        .status(e.getStatus())
        .contentType(MediaType.APPLICATION_JSON)
        .body(new ErrorResponse(e.getStatus(), e.getMessage()));
  }
}
