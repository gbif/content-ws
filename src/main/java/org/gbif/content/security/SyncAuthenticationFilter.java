package org.gbif.content.security;

import org.gbif.content.utils.Paths;
import org.gbif.content.utils.WebApplicationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

@SuppressWarnings("NullableProblems")
@Order(1)
@Component
public class SyncAuthenticationFilter extends OncePerRequestFilter {

  private static final String BEARER_PREFIX = "Bearer ";
  private static final String DEFAULT_USER = "WebHookSync";

  private final String syncToken;

  public SyncAuthenticationFilter(@Value("${content.synchronization.token}") String syncToken) {
    Objects.requireNonNull(syncToken, "Synchronization token can't be null");
    this.syncToken =  syncToken;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    String requestSyncToken = null;
    String path = request.getRequestURI().toLowerCase();

    if (isSyncRequest(path)) {
      String header = request.getHeader(HttpHeaders.AUTHORIZATION);

      if (header != null && header.startsWith(BEARER_PREFIX)) {
        requestSyncToken = header.replace(BEARER_PREFIX, "");
      }

      if (!syncToken.equals(requestSyncToken)) {
        throw new WebApplicationException("Token does not match", HttpStatus.UNAUTHORIZED);
      }
    }

    filterChain.doFilter(request, response);
  }

  private boolean isSyncRequest(String path) {
    return path.contains(Paths.SYNC_RESOURCE_PATH);
  }
}
