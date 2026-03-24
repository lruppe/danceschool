package ch.ruppen.danceschool.shared.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

public final class JwtCookieUtil {

    public static final String COOKIE_NAME = "AUTH_TOKEN";

    private JwtCookieUtil() {
    }

    public static void setTokenCookie(HttpServletResponse response, String token, long maxAgeDays, boolean secure) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(COOKIE_NAME, token)
                .httpOnly(true)
                .path("/")
                .maxAge(Duration.ofDays(maxAgeDays));

        if (secure) {
            builder.secure(true).sameSite("None");
        } else {
            builder.sameSite("Lax");
        }

        response.addHeader("Set-Cookie", builder.build().toString());
    }

    public static void clearTokenCookie(HttpServletResponse response, boolean secure) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(COOKIE_NAME, "")
                .httpOnly(true)
                .path("/")
                .maxAge(0);

        if (secure) {
            builder.secure(true).sameSite("None");
        } else {
            builder.sameSite("Lax");
        }

        response.addHeader("Set-Cookie", builder.build().toString());
    }

    public static Optional<String> extractToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
