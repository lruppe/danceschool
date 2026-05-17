package ch.ruppen.danceschool.dev;

import ch.ruppen.danceschool.course.CourseLifecycleStatus;
import ch.ruppen.danceschool.course.CourseStatusDerivation;
import ch.ruppen.danceschool.enrollment.EnrollmentStatus;
import java.time.LocalDate;
import ch.ruppen.danceschool.school.School;
import ch.ruppen.danceschool.user.AppUser;
import ch.ruppen.danceschool.user.UserService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs with {@code app.demo.enabled=true} so the {@code @TransactionalEventListener} in
 * {@link DemoSchoolSeeder} is active. {@link DirtiesContext} keeps the override from leaking
 * the demo-seeding behavior into other integration tests, which assume the default {@code false}.
 */
@SpringBootTest
@TestPropertySource(properties = "app.demo.enabled=true")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DemoSchoolSeederTest {

    @Autowired
    private DemoSchoolSeeder demoSchoolSeeder;

    @Autowired
    private UserService userService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate tx;

    @Test
    void firstFirebaseLogin_seedsFullDemoSchool() {
        // Production path: a brand-new Firebase user authenticates, UserService creates
        // the user, the AFTER_COMMIT listener fires and seeds a populated demo school.
        AppUser created = tx.execute(s -> userService.findOrCreateByFirebaseUid(
                "uid-end-to-end", "e2e@example.com", "E2E User"));

        tx.executeWithoutResult(s -> {
            List<School> schools = entityManager.createQuery(
                            "SELECT sm.school FROM SchoolMember sm WHERE sm.user.id = :uid",
                            School.class)
                    .setParameter("uid", created.getId())
                    .getResultList();
            assertThat(schools)
                    .as("listener should have provisioned exactly one demo school for the new user")
                    .hasSize(1);
            School school = schools.get(0);
            assertThat(school.getName()).isEqualTo("Demo Dance School");

            Long courseCount = entityManager.createQuery(
                            "SELECT COUNT(c) FROM Course c WHERE c.school = :s", Long.class)
                    .setParameter("s", school)
                    .getSingleResult();
            assertThat(courseCount).isEqualTo(7L);

            Long studentCount = entityManager.createQuery(
                            "SELECT COUNT(st) FROM Student st WHERE st.school = :s", Long.class)
                    .setParameter("s", school)
                    .getSingleResult();
            assertThat(studentCount).isEqualTo(7L);

            // Every lifecycle state must be represented — the headline AC of the issue.
            List<ch.ruppen.danceschool.course.Course> courses = entityManager.createQuery(
                            "SELECT c FROM Course c WHERE c.school = :s",
                            ch.ruppen.danceschool.course.Course.class)
                    .setParameter("s", school)
                    .getResultList();
            LocalDate today = LocalDate.now();
            Set<CourseLifecycleStatus> states = EnumSet.noneOf(CourseLifecycleStatus.class);
            for (var c : courses) {
                states.add(CourseStatusDerivation.deriveStatus(
                        c.getPublishedAt(), c.getStartDate(), c.getEndDate(), today));
            }
            assertThat(states).contains(CourseLifecycleStatus.DRAFT,
                    CourseLifecycleStatus.OPEN,
                    CourseLifecycleStatus.RUNNING,
                    CourseLifecycleStatus.FINISHED);

            Long pendingApprovals = enrollmentCount(school, EnrollmentStatus.PENDING_APPROVAL);
            assertThat(pendingApprovals)
                    .as("at least one pending approval so the Approve tab has content")
                    .isGreaterThan(0L);

            Long openPayments = enrollmentCount(school, EnrollmentStatus.PENDING_PAYMENT);
            assertThat(openPayments).as("open payments visible on Payments page").isGreaterThan(0L);

            Long completedPayments = entityManager.createQuery(
                            "SELECT COUNT(e) FROM Enrollment e WHERE e.course.school = :s "
                                    + "AND e.status = :st AND e.paidAt IS NOT NULL",
                            Long.class)
                    .setParameter("s", school)
                    .setParameter("st", EnrollmentStatus.CONFIRMED)
                    .getSingleResult();
            assertThat(completedPayments)
                    .as("completed payments visible on Payments page")
                    .isGreaterThan(0L);
        });
    }

    @Test
    void seedDemoSchoolFor_isIdempotent_whenUserAlreadyOwnsASchool() {
        AppUser owner = tx.execute(s -> userService.findOrCreateByFirebaseUid(
                "uid-idempotent", "idem@example.com", "Idem Caller"));

        // The listener already seeded a school on user creation. A second explicit call
        // must not produce a second school — production code re-enters this method via the
        // same Firebase user authenticating again.
        tx.executeWithoutResult(s -> demoSchoolSeeder.seedDemoSchoolFor(owner, "Demo Dance School", "Zurich"));

        Long schoolCount = tx.execute(s -> entityManager.createQuery(
                        "SELECT COUNT(sm) FROM SchoolMember sm WHERE sm.user.id = :uid", Long.class)
                .setParameter("uid", owner.getId())
                .getSingleResult());
        assertThat(schoolCount).isEqualTo(1L);
    }

    private Long enrollmentCount(School school, EnrollmentStatus status) {
        return entityManager.createQuery(
                        "SELECT COUNT(e) FROM Enrollment e WHERE e.course.school = :s AND e.status = :st",
                        Long.class)
                .setParameter("s", school)
                .setParameter("st", status)
                .getSingleResult();
    }
}
