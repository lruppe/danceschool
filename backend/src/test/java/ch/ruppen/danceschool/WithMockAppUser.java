package ch.ruppen.danceschool;

import ch.ruppen.danceschool.shared.security.AuthenticatedUser;
import ch.ruppen.danceschool.shared.security.DevAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Installs an {@link AuthenticatedUser} principal wrapped in a {@link DevAuthenticationToken},
 * matching what the dev-auth filter emits under the dev profile. Use on tests that only need
 * a synthesized principal (e.g. to exercise the membership gate without persisting a user).
 * Tests that care about the user row existing in the database should continue to build it
 * and attach the principal via {@code .with(authentication(...))}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@WithSecurityContext(factory = WithMockAppUser.Factory.class)
public @interface WithMockAppUser {

    long userId() default 1L;

    String email() default "test@example.com";

    class Factory implements WithSecurityContextFactory<WithMockAppUser> {
        @Override
        public SecurityContext createSecurityContext(WithMockAppUser annotation) {
            AuthenticatedUser principal = new AuthenticatedUser(
                    annotation.userId(), annotation.email(), null);
            DevAuthenticationToken token = new DevAuthenticationToken(
                    principal, AuthorityUtils.createAuthorityList("ROLE_USER"));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(token);
            return context;
        }
    }
}
