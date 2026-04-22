package ch.ruppen.danceschool.shared.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker for {@code @RestController} classes whose endpoints are gated on school
 * membership. Apply at the class level to every controller that requires an admin
 * (school-member) caller — i.e., any controller that reads or mutates a tenant's
 * {@code Course}, {@code Student}, {@code Enrollment}, or school-scoped aggregate.
 * <p>
 * The annotation itself is inert at runtime. Its purpose is to give the ArchUnit
 * guardrails in {@code ch.ruppen.danceschool.archunit.AdminAuthzArchTest} a stable,
 * explicit target set, replacing the brittle "all {@code *Controller} except an
 * allowlist" pattern. Adding {@code @TenantScoped} to a new controller automatically
 * opts it in to the whole set of authz rules: every public method must carry
 * {@code @PreAuthorize}, the controller must not reach past services into
 * repositories, and it must not call unscoped finders on tenant entities.
 * <p>
 * Controllers that intentionally expose open endpoints (e.g., {@code UserController}'s
 * {@code /api/auth/me}, or {@code SchoolOnboardingController} which bootstraps the tenant
 * before any membership exists) are <b>not</b> annotated. See PRD #226.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface TenantScoped {}
