package ch.ruppen.danceschool.shared.security;

import ch.ruppen.danceschool.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FirebaseJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserService userService;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String firebaseUid = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");

        var appUser = userService.findOrCreateByFirebaseUid(firebaseUid, email, name);
        var principal = new AuthenticatedUser(appUser.getId(), appUser.getEmail());

        var token = new JwtAuthenticationToken(jwt, AuthorityUtils.createAuthorityList("ROLE_USER"));
        return new FirebaseAuthenticationToken(principal, jwt, token.getAuthorities());
    }
}
