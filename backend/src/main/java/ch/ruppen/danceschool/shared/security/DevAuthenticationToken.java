package ch.ruppen.danceschool.shared.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * Authentication token for local development. Carries an {@link AuthenticatedUser} as
 * principal so that {@code @AuthenticationPrincipal AuthenticatedUser} works identically
 * to the production {@link FirebaseAuthenticationToken}.
 */
public class DevAuthenticationToken extends AbstractAuthenticationToken {

    private final AuthenticatedUser authenticatedUser;

    public DevAuthenticationToken(AuthenticatedUser authenticatedUser,
                                  Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.authenticatedUser = authenticatedUser;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public AuthenticatedUser getPrincipal() {
        return authenticatedUser;
    }
}
