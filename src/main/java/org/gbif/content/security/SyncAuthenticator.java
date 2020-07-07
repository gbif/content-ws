package org.gbif.content.security;

import java.io.IOException;
import java.security.Principal;

import javax.ws.rs.container.ContainerRequestContext;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.sun.security.auth.UserPrincipal;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.Authorizer;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import org.gbif.content.utils.Paths;

/**
 * Provides a basic authenticator against security token.
 * This authenticator prevents any user to use the url and trigger content crawls.
 */
public class SyncAuthenticator implements Authenticator<String, Principal> {

  /**
   * Simple authorizer.
   * Authorizes all authenticated users.
   */
  private static class SyncAuthorizer implements Authorizer<Principal> {
    @Override
    public boolean authorize(Principal principal, String role) {
      return principal.getName().equals(DEFAULT_USER);
    }
  }

  //GBIF Security realm
  public static final String GBIF_REALM = "GBIF";

  public static final String BEARER_PREFIX = "Bearer";


  //Default authenticated user name
  private static final String DEFAULT_USER = "WebHookSync";

  //Syncronization authentication token, same token used in the Jenkins Job
  private final String syncToken;

  /**
   * Full constructor. Requires the authentication token only.
   * @param syncToken Syncronization authentication token, same token used in the Jenkins Job
   */
  public SyncAuthenticator(String syncToken) {
    Preconditions.checkNotNull(syncToken, "Synchronization token can't be null");
    this.syncToken = syncToken;
  }

  /**
   * Authenticates the credentials parameter against the security token.
   * @param credentials synchronization token
   * @return an UserPrincipal instance if the provided credential are valid
   * @throws AuthenticationException
   */
  @Override
  public Optional<Principal> authenticate(String credentials) throws AuthenticationException {
    return syncToken.equals(credentials) ? Optional.of(new UserPrincipal(DEFAULT_USER)): Optional.absent();
  }

  /**
   * Factory method that creates a new instances of this authenticator.
   * @param token security token
   * @return an instance of a OAuthFilter that uses this authenticator
   */
  public static AuthFilter<String,Principal> buildAuthFilter(String token) {
    return new ContentAuth(new OAuthCredentialAuthFilter.Builder<>()
      .setAuthenticator(new SyncAuthenticator(token))
      .setRealm(GBIF_REALM)
      .setPrefix(BEARER_PREFIX)
      .setAuthorizer(new SyncAuthorizer())
      .buildAuthFilter());
  }

  /**
   * AuthFilter that authenticates the sync resource only.
   */
  private static class ContentAuth extends AuthFilter<String, Principal> {

    private final OAuthCredentialAuthFilter<?> oAuthFilter;

    public ContentAuth(OAuthCredentialAuthFilter<?> oAuthFilter) {
      this.oAuthFilter = oAuthFilter;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
      if (requestContext.getUriInfo().getRequestUri().getPath().contains(Paths.SYNC_RESOURCE_PATH)) {
        oAuthFilter.filter(requestContext);
      }
    }
  }
}
