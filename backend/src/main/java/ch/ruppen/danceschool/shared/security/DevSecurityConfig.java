package ch.ruppen.danceschool.shared.security;

import ch.ruppen.danceschool.schoolmember.SchoolMemberService;
import ch.ruppen.danceschool.shared.logging.TenantContextFilter;
import ch.ruppen.danceschool.user.AppUser;
import ch.ruppen.danceschool.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Security configuration for local development. Uses form login with in-memory users
 * and session-based authentication instead of Firebase JWT.
 * <p>
 * Active when {@code app.security.dev-auth} is {@code true} (the default for local development).
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(AppSecurityProperties.class)
@ConditionalOnProperty(name = "app.security.dev-auth", havingValue = "true")
@RequiredArgsConstructor
public class DevSecurityConfig {

    private final AppSecurityProperties securityProperties;
    private final UserService userService;
    private final SchoolMemberService schoolMemberService;
    private final TenantContextFilter tenantContextFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler(devAuthSuccessHandler())
                        .failureHandler((req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpServletResponse.SC_OK))
                        .invalidateHttpSession(true))
                .addFilterAfter(tenantContextFilter, AuthorizationFilter.class);

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        var owner = User.builder()
                .username("owner@test.com")
                .password(passwordEncoder.encode("password"))
                .roles("USER")
                .build();
        var owner2 = User.builder()
                .username("owner2@test.com")
                .password(passwordEncoder.encode("password"))
                .roles("USER")
                .build();
        return new InMemoryUserDetailsManager(owner, owner2);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return org.springframework.security.crypto.factory.PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    private AuthenticationSuccessHandler devAuthSuccessHandler() {
        return (HttpServletRequest request, HttpServletResponse response, Authentication authentication) -> {
            String email = authentication.getName();
            AppUser appUser = userService.findByEmail(email)
                    .orElseThrow(() -> new IllegalStateException("Dev user not found: " + email));

            Long schoolId = schoolMemberService.findSchoolIdByUserId(appUser.getId()).orElse(null);
            var principal = new AuthenticatedUser(appUser.getId(), appUser.getEmail(), schoolId);
            var devToken = new DevAuthenticationToken(
                    principal, AuthorityUtils.createAuthorityList("ROLE_USER"));

            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(devToken);
            SecurityContextHolder.setContext(context);

            new HttpSessionSecurityContextRepository()
                    .saveContext(context, request, response);

            response.setStatus(HttpServletResponse.SC_OK);
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(securityProperties.corsAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
