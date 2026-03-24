package ch.ruppen.danceschool.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public record AppSecurityProperties(
        List<String> corsAllowedOrigins,
        String frontendUrl,
        boolean secureCookies
) {
}
