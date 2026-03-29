package ch.ruppen.danceschool.shared.security;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.PlainJWT;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provides a permissive JwtDecoder for local development with the Firebase Auth emulator.
 * Emulator-issued tokens use {@code "alg": "none"} (unsigned) or are signed with a local
 * key — neither can be verified against Firebase's real JWKS endpoint. This decoder parses
 * the token without verifying the signature.
 * <p>
 * Only activates when no other JwtDecoder bean exists — i.e., when neither the prod
 * profile (issuer-uri auto-config) nor test config (TestSecurityConfig) provides one.
 */
@Configuration
class EmulatorJwtDecoderConfig {

    @Bean
    @ConditionalOnMissingBean
    JwtDecoder jwtDecoder() {
        return token -> {
            try {
                JWT parsed = JWTParser.parse(token);
                Map<String, Object> headers;
                Map<String, Object> claims;

                if (parsed instanceof SignedJWT signed) {
                    headers = Map.copyOf(signed.getHeader().toJSONObject());
                    claims = signed.getJWTClaimsSet().getClaims();
                } else if (parsed instanceof PlainJWT plain) {
                    headers = Map.copyOf(plain.getHeader().toJSONObject());
                    claims = plain.getJWTClaimsSet().getClaims();
                } else {
                    throw new JwtException("Unsupported token type");
                }

                // Convert Date values to Instant — Nimbus parses timestamps as Date,
                // but Spring's Jwt.Builder requires Instant.
                Map<String, Object> convertedClaims = new LinkedHashMap<>(claims);
                convertedClaims.replaceAll((k, v) -> v instanceof Date d ? d.toInstant() : v);

                return Jwt.withTokenValue(token)
                        .headers(h -> h.putAll(headers))
                        .claims(c -> c.putAll(convertedClaims))
                        .build();
            } catch (ParseException e) {
                throw new JwtException("Failed to parse JWT", e);
            }
        };
    }
}
