package ch.ruppen.danceschool.shared.logging;

import ch.ruppen.danceschool.shared.security.AuthenticatedUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Populates MDC with {@code schoolId} and {@code userId} for the duration of each request so
 * every log line emitted in the request scope — including framework logs — carries tenant
 * context. Registered after Spring Security's {@code AuthorizationFilter} so the principal is
 * already on the {@link SecurityContextHolder}.
 * <p>
 * MDC is cleared in a {@code finally} block to prevent leakage across thread-pool reuse.
 * <p>
 * Spring Boot auto-registers any {@code OncePerRequestFilter} bean on the main servlet chain
 * (outside {@code springSecurityFilterChain}), which would run before authentication and the
 * {@code ALREADY_FILTERED} guard would then suppress the in-security-chain invocation. A
 * companion {@code FilterRegistrationBean} with {@code enabled=false} disables that
 * auto-registration so only the explicit {@code addFilterAfter(...)} wiring runs.
 */
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    public static final String MDC_SCHOOL_ID = "schoolId";
    public static final String MDC_USER_ID = "userId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            populateMdc();
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_SCHOOL_ID);
            MDC.remove(MDC_USER_ID);
        }
    }

    private void populateMdc() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            return;
        }
        MDC.put(MDC_USER_ID, String.valueOf(user.userId()));
        if (user.schoolId() != null) {
            MDC.put(MDC_SCHOOL_ID, String.valueOf(user.schoolId()));
        }
    }
}
