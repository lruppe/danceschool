package ch.ruppen.danceschool.archunit;

import ch.ruppen.danceschool.shared.security.TenantScoped;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Mechanical guardrails for admin authorization. These rules fail the build when a change
 * silently weakens tenant isolation — e.g., a new {@code @RestController} that forgets
 * {@code @PreAuthorize}, a controller reaching past services into a repository, or an
 * unscoped {@code findById} on a tenant entity.
 * <p>
 * <b>Context:</b> PRD #226 — Role-based authorization for admin endpoints. Gap identified
 * and scoped in issue #356 (follow-up to #352, which shipped the initial pattern).
 * <p>
 * <b>Canonical examples in the codebase:</b>
 * <ul>
 *     <li>Correct {@code @TenantScoped} class: {@code CourseController}</li>
 *     <li>Correct scoped finder: {@code CourseRepository#findByIdAndSchoolId}</li>
 *     <li>Correct onboarding-style open controller: {@code SchoolOnboardingController}</li>
 * </ul>
 * Every rule below prints a multi-line banner on failure. Read the banner; it tells you
 * exactly which file to change and how.
 */
@AnalyzeClasses(
        packages = "ch.ruppen.danceschool",
        importOptions = ImportOption.DoNotIncludeTests.class)
public class AdminAuthzArchTest {

    private static final String ROOT_PACKAGE = "ch.ruppen.danceschool";

    /** Repositories that manage tenant-owned entities. Unscoped finders on these leak across schools. */
    private static final Set<String> TENANT_REPOSITORIES = Set.of(
            "CourseRepository",
            "StudentRepository",
            "EnrollmentRepository");

    /** Spring Data finders that ignore any {@code schoolId} filter. */
    private static final Set<String> UNSCOPED_FINDER_METHODS = Set.of(
            "findById",
            "existsById",
            "deleteById",
            "getReferenceById");

    /** Tenant entity simple names — the return types that trigger rule 3. */
    private static final Set<String> TENANT_ENTITIES = Set.of(
            "Course",
            "Student",
            "Enrollment");

    /**
     * Collection wrapper types that rule 3 unwraps when inspecting repository return types.
     * A method returning {@code Optional<Course>} or {@code List<Course>} is still an "id lookup
     * that returns a tenant entity" as far as scoping is concerned.
     */
    private static final Set<String> WRAPPER_TYPES = Set.of(
            "Optional",
            "List",
            "Collection",
            "Iterable",
            "Set");

    /**
     * {@code @RestController} classes that intentionally expose open endpoints (no school
     * membership required). Adding to this list is a deliberate decision — document the
     * reason in the controller's Javadoc.
     */
    private static final Set<String> OPEN_CONTROLLER_ALLOWLIST = Set.of(
            "UserController",             // GET /api/auth/me — any authenticated user
            "SchoolOnboardingController"  // POST /api/schools — bootstraps the first school
    );

    // ──────────────────────────────────────────────────────────────────────────────────
    // Rule 1 — controllers must not reach an unscoped finder on a tenant entity
    // ──────────────────────────────────────────────────────────────────────────────────

    /**
     * No {@code @TenantScoped} class may transitively call {@code findById}/{@code existsById}/
     * {@code deleteById}/{@code getReferenceById} on the tenant repositories. PRD #226; canonical
     * scoped finder: {@link ch.ruppen.danceschool.course.CourseRepository#findByIdAndSchoolId}.
     */
    @ArchTest
    public static final ArchRule tenant_scoped_controllers_must_not_call_unscoped_finders_on_tenant_entities =
            classes()
                    .that().areAnnotatedWith(TenantScoped.class)
                    .should(new ArchCondition<JavaClass>(
                            "not reach findById/existsById/deleteById/getReferenceById on CourseRepository, StudentRepository, or EnrollmentRepository through any transitive call") {
                        @Override
                        public void check(JavaClass controller, ConditionEvents events) {
                            for (JavaMethod entry : controller.getMethods()) {
                                for (Violation v : findUnscopedFinderReachedFrom(entry)) {
                                    events.add(SimpleConditionEvent.violated(controller,
                                            banner(
                                                    "TENANT ISOLATION VIOLATION — unscoped repository call from controller",
                                                    renderChain(v.chain) + "   ← FORBIDDEN",
                                                    "replace " + v.target + " with the *AndSchoolId scoped variant"
                                                            + " (see CourseRepository.findByIdAndSchoolId).",
                                                    "any @TenantScoped controller path that resolves a Course/Student/Enrollment by id"
                                                            + " must go through a schoolId-filtered query. Unscoped finders are a cross-tenant data-leak risk.",
                                                    "EnrollmentService.confirmPayment"
                                            )));
                                }
                            }
                        }
                    });

    // ──────────────────────────────────────────────────────────────────────────────────
    // Rule 2 — every public method on a @TenantScoped class must carry @PreAuthorize
    // ──────────────────────────────────────────────────────────────────────────────────

    /**
     * Every public method on a {@code @TenantScoped} class must carry {@code @PreAuthorize}
     * (directly or inherited from the class). PRD #226; canonical example: {@code CourseController}
     * uses a class-level {@code @PreAuthorize("@schoolAuthz.hasMembership()")} covering all methods.
     */
    @ArchTest
    public static final ArchRule tenant_scoped_controller_methods_must_declare_preauthorize =
            classes()
                    .that().areAnnotatedWith(TenantScoped.class)
                    .should(new ArchCondition<JavaClass>(
                            "have @PreAuthorize on every public method (directly or via class-level annotation)") {
                        @Override
                        public void check(JavaClass controller, ConditionEvents events) {
                            boolean classLevel = controller.isAnnotatedWith(PreAuthorize.class);
                            for (JavaMethod m : controller.getMethods()) {
                                if (!m.getModifiers().contains(JavaModifier.PUBLIC)) continue;
                                if (classLevel || m.isAnnotatedWith(PreAuthorize.class)) continue;

                                events.add(SimpleConditionEvent.violated(m,
                                        banner(
                                                "AUTHZ VIOLATION — @TenantScoped method without @PreAuthorize",
                                                controller.getSimpleName() + "." + m.getName() + "(…)"
                                                        + "   ← MISSING @PreAuthorize",
                                                "add @PreAuthorize(\"@schoolAuthz.hasMembership()\") to the method,"
                                                        + " or apply it at the class level for all methods at once.",
                                                "every @TenantScoped controller method is a membership-gated admin endpoint."
                                                        + " Missing @PreAuthorize means any authenticated user reaches it.",
                                                "CourseController (class-level @PreAuthorize covers all methods)"
                                        )));
                            }
                        }
                    });

    // ──────────────────────────────────────────────────────────────────────────────────
    // Rule 3 — id-taking repository methods returning a tenant entity must also take schoolId
    // ──────────────────────────────────────────────────────────────────────────────────

    /**
     * Any {@code *Repository} method that returns a {@code Course}/{@code Student}/{@code Enrollment}
     * (or an {@code Optional}/{@code List}/etc. of one) and accepts a {@code Long id} must also
     * accept a {@code Long schoolId}. PRD #226; canonical example:
     * {@link ch.ruppen.danceschool.course.CourseRepository#findByIdAndSchoolId}.
     */
    @ArchTest
    public static final ArchRule admin_id_taking_repository_methods_must_be_tenant_scoped =
            methods()
                    .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Repository")
                    .and().areDeclaredInClassesThat().resideInAPackage(ROOT_PACKAGE + "..")
                    .should(new ArchCondition<JavaMethod>(
                            "accept a Long schoolId parameter whenever they return a Course/Student/Enrollment and take a Long id") {
                        @Override
                        public void check(JavaMethod m, ConditionEvents events) {
                            if (!returnsTenantEntity(m)) return;
                            if (!hasLongParameterNamed(m, "id")) return;
                            if (hasLongParameterNamed(m, "schoolId")) return;

                            events.add(SimpleConditionEvent.violated(m,
                                    banner(
                                            "TENANT ISOLATION VIOLATION — id-taking repository method missing schoolId",
                                            m.getOwner().getSimpleName() + "." + m.getName() + "(Long id, …)"
                                                    + "   ← MISSING Long schoolId",
                                            "add a Long schoolId parameter and filter on it — e.g., findByIdAndSchoolId(id, schoolId).",
                                            "a Repository returning a tenant entity from a bare id is a cross-tenant data-leak"
                                                    + " waiting to happen — any caller can fetch another tenant's row.",
                                            "CourseRepository.findByIdAndSchoolId"
                                    )));
                        }
                    });

    // ──────────────────────────────────────────────────────────────────────────────────
    // Rule 4 — controllers must not depend on repositories directly
    // ──────────────────────────────────────────────────────────────────────────────────

    /**
     * {@code @TenantScoped} classes must not depend on any {@code *Repository} — data access
     * belongs in a service. Enforces Architectural Rules 3 &amp; 4 (see backend/src/CLAUDE.md) as a
     * build-time check. PRD #226; canonical example: {@code CourseController} injects
     * {@code CourseService}, not {@code CourseRepository}.
     */
    @ArchTest
    public static final ArchRule tenant_scoped_controllers_must_not_call_repositories_directly =
            noClasses()
                    .that().areAnnotatedWith(TenantScoped.class)
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
                    .because(banner(
                            "LAYERING VIOLATION — @TenantScoped controller depends on a Repository",
                            "controller injects / references a *Repository type directly",
                            "move the data access into a Service. Controllers should be pure HTTP adapters that call one"
                                    + " service method. See backend/src/CLAUDE.md § Architectural Rules 3 & 4.",
                            "scoping logic lives in services. Letting controllers reach past services into repositories"
                                    + " lets a caller bypass the scoped-finder discipline.",
                            "CourseController → CourseService (not CourseRepository)"
                    ));

    // ──────────────────────────────────────────────────────────────────────────────────
    // Rule 5 — every @RestController is either @TenantScoped or on the open-controller allowlist
    // ──────────────────────────────────────────────────────────────────────────────────

    /**
     * Every {@code @RestController} under {@code ch.ruppen.danceschool} must either carry
     * {@code @TenantScoped} or appear on {@link #OPEN_CONTROLLER_ALLOWLIST}. Forces an explicit
     * authz classification when a new controller appears. PRD #226; canonical examples:
     * {@code CourseController} ({@code @TenantScoped}) and {@code SchoolOnboardingController}
     * (allowlisted, open).
     */
    @ArchTest
    public static final ArchRule rest_controllers_must_be_marked_tenant_scoped_or_allowlisted =
            classes()
                    .that().areAnnotatedWith(RestController.class)
                    .and().resideInAPackage(ROOT_PACKAGE + "..")
                    .should(new ArchCondition<JavaClass>(
                            "carry @TenantScoped or appear on the explicit open-controller allowlist") {
                        @Override
                        public void check(JavaClass controller, ConditionEvents events) {
                            if (controller.isAnnotatedWith(TenantScoped.class)) return;
                            if (OPEN_CONTROLLER_ALLOWLIST.contains(controller.getSimpleName())) return;

                            events.add(SimpleConditionEvent.violated(controller,
                                    banner(
                                            "AUTHZ CLASSIFICATION MISSING — @RestController is neither @TenantScoped nor allowlisted",
                                            controller.getName() + "   ← needs an explicit authz classification",
                                            "either add @TenantScoped (admin endpoint, gated by school membership) or"
                                                    + " add the class simple name to AdminAuthzArchTest.OPEN_CONTROLLER_ALLOWLIST"
                                                    + " with a short justification in the controller's Javadoc.",
                                            "this rule forces an explicit decision whenever a new controller appears."
                                                    + " A silent @RestController is how the tenant-isolation contract quietly erodes.",
                                            "CourseController (@TenantScoped) vs SchoolOnboardingController (on the allowlist)"
                                    )));
                        }
                    });

    // ──────────────────────────────────────────────────────────────────────────────────
    // Rule 6 — @PreAuthorize referencing @schoolAuthz.hasMembership() must be on @TenantScoped
    // ──────────────────────────────────────────────────────────────────────────────────

    /**
     * Any class carrying class-level {@code @PreAuthorize} whose value references
     * {@code @schoolAuthz.hasMembership()} must also be {@code @TenantScoped}. Catches the
     * membership-gate SpEL copy-pasted onto an open controller, which would silently break
     * onboarding. PRD #226; canonical example: {@code CourseController} carries both.
     */
    @ArchTest
    public static final ArchRule preauthorize_with_has_membership_must_be_on_tenant_scoped_class =
            classes()
                    .that().resideInAPackage(ROOT_PACKAGE + "..")
                    .should(new ArchCondition<JavaClass>(
                            "carry @TenantScoped whenever @PreAuthorize references @schoolAuthz.hasMembership()") {
                        @Override
                        public void check(JavaClass clazz, ConditionEvents events) {
                            if (!hasClassLevelHasMembershipPreAuthorize(clazz)) return;
                            if (clazz.isAnnotatedWith(TenantScoped.class)) return;

                            events.add(SimpleConditionEvent.violated(clazz,
                                    banner(
                                            "AUTHZ MISMATCH — membership gate without @TenantScoped marker",
                                            clazz.getName() + "   ← @PreAuthorize(\"@schoolAuthz.hasMembership()\") but not @TenantScoped",
                                            "add @TenantScoped to the class. If this controller is genuinely open, remove"
                                                    + " the @PreAuthorize; if it is an admin endpoint, the marker belongs there"
                                                    + " so the rest of the ArchUnit rules apply.",
                                            "the membership-gate SpEL only makes sense for admin/tenant-scoped surface area."
                                                    + " Copy-pasting it onto an open controller silently breaks onboarding.",
                                            "CourseController carries both @PreAuthorize and @TenantScoped"
                                    )));
                        }
                    });

    // ──────────────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────────────

    /**
     * BFS across {@code entry}'s transitive method calls, recording the call chain to every
     * unscoped finder on a tenant repository. Returns one {@link Violation} per reached
     * forbidden call (deduped by call path's final element).
     */
    private static List<Violation> findUnscopedFinderReachedFrom(JavaMethod entry) {
        List<Violation> violations = new ArrayList<>();
        Set<JavaMethod> visited = new HashSet<>();
        // parent map lets us reconstruct the call chain when we hit a violation
        Map<JavaMethod, JavaMethod> parent = new LinkedHashMap<>();
        Deque<JavaMethod> queue = new ArrayDeque<>();
        queue.add(entry);
        visited.add(entry);

        while (!queue.isEmpty()) {
            JavaMethod current = queue.poll();
            for (JavaMethodCall call : current.getMethodCallsFromSelf()) {
                String targetOwner = call.getTargetOwner().getSimpleName();
                String targetName = call.getName();

                if (TENANT_REPOSITORIES.contains(targetOwner)
                        && UNSCOPED_FINDER_METHODS.contains(targetName)) {
                    List<String> chain = reconstructChain(entry, current, parent);
                    chain.add(targetOwner + "." + targetName + "(…)");
                    violations.add(new Violation(chain, targetOwner + "." + targetName));
                    continue;
                }

                // Only recurse into methods declared in our own package — avoids exploring
                // the whole JDK / Spring when we just want admin-path reachability.
                JavaMethod resolved = resolveOwnMethod(call);
                if (resolved != null && visited.add(resolved)) {
                    parent.put(resolved, current);
                    queue.add(resolved);
                }
            }
        }
        return violations;
    }

    private static JavaMethod resolveOwnMethod(JavaMethodCall call) {
        if (!call.getTargetOwner().getPackageName().startsWith(ROOT_PACKAGE)) return null;
        return call.getTarget().resolveMember().orElse(null) instanceof JavaMethod jm ? jm : null;
    }

    private static List<String> reconstructChain(JavaMethod entry, JavaMethod current, Map<JavaMethod, JavaMethod> parent) {
        Deque<JavaMethod> stack = new ArrayDeque<>();
        for (JavaMethod m = current; m != null; m = parent.get(m)) stack.push(m);
        List<String> chain = new ArrayList<>();
        for (JavaMethod m : stack) {
            chain.add(m.getOwner().getSimpleName() + "." + m.getName() + "(…)");
        }
        return chain;
    }

    private static String renderChain(List<String> chain) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chain.size(); i++) {
            if (i == 0) sb.append("  ").append(chain.get(i));
            else sb.append("\n      → ").append(chain.get(i));
        }
        return sb.toString();
    }

    private static boolean returnsTenantEntity(JavaMethod m) {
        JavaClass raw = m.getRawReturnType();
        String rawName = raw.getSimpleName();
        if (TENANT_ENTITIES.contains(rawName)) return true;
        if (!WRAPPER_TYPES.contains(rawName)) return false;
        // Inspect the first generic argument via reflection — ArchUnit's JavaType exposes
        // the signature but reflection is simpler here.
        try {
            Method reflected = m.reflect();
            java.lang.reflect.Type generic = reflected.getGenericReturnType();
            String s = generic.getTypeName();
            for (String entity : TENANT_ENTITIES) {
                if (s.contains("." + entity + ">") || s.contains("." + entity + ",")) return true;
            }
        } catch (Throwable ignored) {
            // Reflection miss (e.g., class not loadable) — fall through to false. Worst case the
            // rule is a bit less strict; it still catches the raw-return-type cases.
        }
        return false;
    }

    private static boolean hasLongParameterNamed(JavaMethod method, String name) {
        try {
            Method reflected = method.reflect();
            for (Parameter p : reflected.getParameters()) {
                if (!p.isNamePresent()) continue; // -parameters not enabled for this class
                if (!Long.class.equals(p.getType()) && !long.class.equals(p.getType())) continue;
                if (p.getName().equals(name)) return true;
            }
        } catch (Throwable ignored) {
            // Reflection miss (class not loadable, etc.). Fall through to false — when looking for
            // `id`, this means rule 3 doesn't apply (safe); when looking for `schoolId`, rule 3 fires
            // on the assumption the param isn't there (also safe).
        }
        return false;
    }

    private static boolean hasClassLevelHasMembershipPreAuthorize(JavaClass clazz) {
        if (!clazz.isAnnotatedWith(PreAuthorize.class)) return false;
        String value = clazz.getAnnotationOfType(PreAuthorize.class).value();
        return value != null && value.contains("@schoolAuthz.hasMembership()");
    }

    private static String banner(String title, String body, String fix, String why, String example) {
        String bar = "══════════════════════════════════════════════════════════════════════";
        return "\n" + bar + "\n"
                + "  " + title + "\n"
                + bar + "\n\n"
                + body + "\n\n"
                + "  Why: " + why + "\n"
                + "  Fix: " + fix + "\n"
                + "  Context: PRD #226 — Role-based authorization for admin endpoints\n"
                + "  Pattern: see " + example + "\n"
                + bar;
    }

    private record Violation(List<String> chain, String target) {}
}
