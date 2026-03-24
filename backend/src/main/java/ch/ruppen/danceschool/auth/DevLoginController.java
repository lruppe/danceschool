package ch.ruppen.danceschool.auth;

import ch.ruppen.danceschool.shared.security.AppSecurityProperties;
import ch.ruppen.danceschool.shared.security.JwtCookieUtil;
import ch.ruppen.danceschool.shared.security.JwtProperties;
import ch.ruppen.danceschool.shared.security.JwtUtil;
import ch.ruppen.danceschool.user.AppUser;
import ch.ruppen.danceschool.user.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev")
@Profile("!prod")
@RequiredArgsConstructor
public class DevLoginController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final AppSecurityProperties securityProperties;

    @PostMapping("/login")
    public ResponseEntity<Void> devLogin(@RequestBody DevLoginRequest request, HttpServletResponse response) {
        AppUser user = userService.findOrCreateOAuthUser("dev", request.email(), request.email(), "Dev User", null);

        String token = jwtUtil.generateToken(user.getId(), user.getEmail());
        JwtCookieUtil.setTokenCookie(response, token, jwtProperties.expirationDays(), securityProperties.secureCookies());

        return ResponseEntity.ok().build();
    }

    record DevLoginRequest(String email) {
    }
}
