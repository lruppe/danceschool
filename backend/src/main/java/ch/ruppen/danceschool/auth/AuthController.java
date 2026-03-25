package ch.ruppen.danceschool.auth;

import ch.ruppen.danceschool.shared.security.AuthenticatedUser;
import ch.ruppen.danceschool.user.UserDto;
import ch.ruppen.danceschool.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final HttpSessionSecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    AuthController(UserService userService, AuthenticationConfiguration authConfig) throws Exception {
        this.userService = userService;
        this.authenticationManager = authConfig.getAuthenticationManager();
    }

    record LoginRequest(String username, String password) {}

    @PostMapping("/login")
    public ResponseEntity<UserDto> login(@RequestBody LoginRequest request,
                                         HttpServletRequest httpRequest,
                                         HttpServletResponse httpResponse) {
        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return userService.findByUsername(auth.getName())
                .map(appUser -> {
                    AuthenticatedUser principal = new AuthenticatedUser(appUser.getId(), appUser.getEmail());
                    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                            principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));

                    SecurityContext context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(token);
                    SecurityContextHolder.setContext(context);
                    securityContextRepository.saveContext(context, httpRequest, httpResponse);

                    return ResponseEntity.ok(userService.toUserDto(appUser));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return userService.findById(principal.userId())
                .map(userService::toUserDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok().build();
    }
}
