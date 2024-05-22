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
package org.gbif.content.security;

import org.gbif.content.utils.Paths;

import java.io.IOException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("NullableProblems")
@Order(1)
@Component
public class SyncAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger LOG = LoggerFactory.getLogger(SyncAuthenticationFilter.class);

  private static final String BEARER_PREFIX = "Bearer ";

  private final String syncToken;

  public SyncAuthenticationFilter(@Value("${content.synchronization.token}") String syncToken) {
    Objects.requireNonNull(syncToken, "Synchronization token can't be null");
    this.syncToken = syncToken;
  }

  @Override
  protected void doFilterInternal(
    HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String requestSyncToken = null;
    String path = request.getRequestURI().toLowerCase();

    if (isSyncRequest(path)) {
      String header = request.getHeader(HttpHeaders.AUTHORIZATION);

      if (header != null && header.startsWith(BEARER_PREFIX)) {
        requestSyncToken = header.replace(BEARER_PREFIX, "");
      }

      if (!syncToken.equals(requestSyncToken)) {
        LOG.warn("Token does not match");
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.TEXT_PLAIN_VALUE);
        response.getWriter().write("Credentials are required to access this resource.");
        return;
      }
    }

    filterChain.doFilter(request, response);
  }

  private boolean isSyncRequest(String path) {
    return path.contains(Paths.SYNC_RESOURCE_PATH);
  }
}
