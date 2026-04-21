package ch.ruppen.danceschool.shared.logging;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Disables Spring Boot's automatic servlet-chain registration of {@link TenantContextFilter}.
 * <p>
 * Without this, the filter would run on the main servlet chain <em>before</em> Spring Security
 * authenticates — seeing an empty {@code SecurityContextHolder} — and then the
 * {@code ALREADY_FILTERED} guard in {@link org.springframework.web.filter.OncePerRequestFilter}
 * would make the intended in-security-chain registration a no-op. The filter is wired into the
 * Spring Security chain explicitly via {@code addFilterAfter(..., AuthorizationFilter.class)}
 * in each {@code SecurityFilterChain} bean.
 */
@Configuration
class TenantContextFilterRegistrationConfig {

    @Bean
    FilterRegistrationBean<TenantContextFilter> tenantContextFilterRegistration(
            TenantContextFilter filter) {
        FilterRegistrationBean<TenantContextFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
