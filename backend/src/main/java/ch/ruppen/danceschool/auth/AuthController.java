package ch.ruppen.danceschool.auth;

import ch.ruppen.danceschool.shared.security.AppSecurityProperties;
import ch.ruppen.danceschool.shared.security.AuthenticatedUser;
import ch.ruppen.danceschool.shared.security.JwtCookieUtil;
import ch.ruppen.danceschool.user.UserDto;
import ch.ruppen.danceschool.user.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AppSecurityProperties securityProperties;

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return userService.findById(principal.userId())
                .map(userService::toUserDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        JwtCookieUtil.clearTokenCookie(response, securityProperties.secureCookies());
        return ResponseEntity.ok().build();
    }
}
