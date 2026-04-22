package ch.ruppen.danceschool.shared.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Enables {@code @PreAuthorize} on controller methods. Kept in a standalone config so both
 * {@link DevSecurityConfig} and {@link SecurityConfig} pick it up regardless of auth mode.
 */
@Configuration
@EnableMethodSecurity
class MethodSecurityConfig {
}
