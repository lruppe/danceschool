package ch.ruppen.danceschool.shared.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;

public class FirebaseAuthenticationToken extends JwtAuthenticationToken {

    private final AuthenticatedUser authenticatedUser;

    public FirebaseAuthenticationToken(AuthenticatedUser authenticatedUser, Jwt jwt,
                                       Collection<? extends GrantedAuthority> authorities) {
        super(jwt, authorities);
        this.authenticatedUser = authenticatedUser;
    }

    @Override
    public AuthenticatedUser getPrincipal() {
        return authenticatedUser;
    }
}
