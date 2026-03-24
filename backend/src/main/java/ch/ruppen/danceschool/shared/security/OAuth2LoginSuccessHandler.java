package ch.ruppen.danceschool.shared.security;

import ch.ruppen.danceschool.user.AppUser;
import ch.ruppen.danceschool.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final AppSecurityProperties securityProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauthUser = oauthToken.getPrincipal();
        String provider = oauthToken.getAuthorizedClientRegistrationId();

        String oauthId = oauthUser.getName();
        String email = oauthUser.getAttribute("email");
        String name = extractName(oauthUser, provider);
        String avatarUrl = extractAvatarUrl(oauthUser, provider);

        AppUser user = userService.findOrCreateOAuthUser(provider, oauthId, email, name, avatarUrl);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        JwtCookieUtil.setTokenCookie(response, token, jwtProperties.expirationDays());

        response.sendRedirect(securityProperties.frontendUrl() + "/auth/callback");
    }

    private String extractName(OAuth2User oauthUser, String provider) {
        return switch (provider) {
            case "github" -> oauthUser.getAttribute("login");
            default -> oauthUser.getAttribute("name");
        };
    }

    private String extractAvatarUrl(OAuth2User oauthUser, String provider) {
        return switch (provider) {
            case "github" -> oauthUser.getAttribute("avatar_url");
            default -> oauthUser.getAttribute("picture");
        };
    }
}
