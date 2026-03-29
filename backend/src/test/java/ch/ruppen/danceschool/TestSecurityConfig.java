package ch.ruppen.danceschool;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;
import java.util.Map;

@TestConfiguration
public class TestSecurityConfig {

    public static final String TEST_FIREBASE_UID = "firebase-uid-123";
    public static final String TEST_EMAIL = "test@example.com";
    public static final String TEST_NAME = "Test User";
    public static final String VALID_TOKEN = "valid-test-token";
    public static final String INVALID_TOKEN = "invalid-token";

    @Bean
    public JwtDecoder jwtDecoder() {
        return token -> {
            if (!VALID_TOKEN.equals(token)) {
                throw new BadJwtException("Invalid token");
            }
            return Jwt.withTokenValue(token)
                    .header("alg", "RS256")
                    .subject(TEST_FIREBASE_UID)
                    .claim("email", TEST_EMAIL)
                    .claim("name", TEST_NAME)
                    .issuer("https://securetoken.google.com/test-project")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
        };
    }
}
