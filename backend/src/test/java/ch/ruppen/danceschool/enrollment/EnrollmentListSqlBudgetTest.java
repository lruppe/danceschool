package ch.ruppen.danceschool.enrollment;

import ch.ruppen.danceschool.TestSecurityConfig;
import ch.ruppen.danceschool.course.Course;
import ch.ruppen.danceschool.course.CourseLevel;
import ch.ruppen.danceschool.course.CourseType;
import ch.ruppen.danceschool.course.DanceStyle;
import ch.ruppen.danceschool.course.PriceModel;
import ch.ruppen.danceschool.course.RecurrenceType;
import ch.ruppen.danceschool.school.School;
import ch.ruppen.danceschool.schoolmember.MemberRole;
import ch.ruppen.danceschool.schoolmember.SchoolMember;
import ch.ruppen.danceschool.shared.security.AuthenticatedUser;
import ch.ruppen.danceschool.student.Student;
import ch.ruppen.danceschool.student.StudentDanceLevel;
import ch.ruppen.danceschool.user.AppUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the enrollment list query does not scale with N via Hibernate statistics.
 * Fixes the N+1 that was lazy-loading {@code student}, {@code student.danceLevels},
 * and {@code course} per row when building the list DTOs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
class EnrollmentListSqlBudgetTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private AppUser owner;
    private School school;
    private Course course;

    @BeforeEach
    void setUp() {
        owner = createUser("owner@example.com", "Owner", "firebase-owner");
        school = createSchoolWithOwner("Test School", owner);

        course = createCourse(school);
        entityManager.flush();
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 20})
    void listEnrollments_sqlCountDoesNotScaleWithN(int n) throws Exception {
        for (int i = 0; i < n; i++) {
            Student s = createStudent(school, "Student " + i, "s" + i + "@example.com");
            addDanceLevel(s, DanceStyle.BACHATA, CourseLevel.INTERMEDIATE);
            addDanceLevel(s, DanceStyle.SALSA, CourseLevel.BEGINNER);
            createEnrollment(course, s);
        }
        entityManager.flush();
        entityManager.clear();

        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        mockMvc.perform(get("/api/courses/{id}/enrollments", course.getId())
                        .with(authentication(authToken(owner))))
                .andExpect(status().isOk());

        // Budget: 1 SchoolAuthz membership check + 1 load the caller's school +
        // 1 load the course (tenant-scoped) + 1 load the enrollments list.
        // The point is O(1) in N, not a specific number.
        assertThat(stats.getPrepareStatementCount())
                .as("SQL budget for /enrollments with N=%d", n)
                .isLessThanOrEqualTo(4);
    }

    private AppUser createUser(String email, String name, String firebaseUid) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setName(name);
        user.setFirebaseUid(firebaseUid);
        entityManager.persist(user);
        return user;
    }

    private School createSchoolWithOwner(String name, AppUser ownerUser) {
        School s = new School();
        s.setName(name);
        entityManager.persist(s);

        SchoolMember member = new SchoolMember();
        member.setUser(ownerUser);
        member.setSchool(s);
        member.setRole(MemberRole.OWNER);
        entityManager.persist(member);

        return s;
    }

    private Course createCourse(School s) {
        Course c = new Course();
        c.setSchool(s);
        c.setTitle("Bachata Intermediate");
        c.setDanceStyle(DanceStyle.BACHATA);
        c.setLevel(CourseLevel.INTERMEDIATE);
        c.setCourseType(CourseType.PARTNER);
        c.setMaxParticipants(50);
        c.setStartDate(LocalDate.now().plusWeeks(2));
        c.setEndDate(LocalDate.now().plusWeeks(10));
        c.setLocation("Studio A");
        c.setTeachers("Test Teacher");
        c.setStartTime(LocalTime.of(19, 0));
        c.setEndTime(LocalTime.of(20, 0));
        c.setDayOfWeek(DayOfWeek.MONDAY);
        c.setRecurrenceType(RecurrenceType.WEEKLY);
        c.setNumberOfSessions(8);
        c.setPriceModel(PriceModel.FIXED_COURSE);
        c.setPrice(new BigDecimal("200.00"));
        c.setPublishedAt(LocalDate.now().minusDays(1));
        entityManager.persist(c);
        return c;
    }

    private Student createStudent(School s, String name, String email) {
        Student st = new Student();
        st.setSchool(s);
        st.setName(name);
        st.setEmail(email);
        entityManager.persist(st);
        return st;
    }

    private void addDanceLevel(Student st, DanceStyle style, CourseLevel level) {
        StudentDanceLevel dl = new StudentDanceLevel();
        dl.setStudent(st);
        dl.setDanceStyle(style);
        dl.setLevel(level);
        st.getDanceLevels().add(dl);
        entityManager.persist(dl);
    }

    private void createEnrollment(Course c, Student s) {
        Enrollment e = new Enrollment();
        e.setCourse(c);
        e.setStudent(s);
        e.setDanceRole(DanceRole.LEAD);
        e.setStatus(EnrollmentStatus.CONFIRMED);
        e.setEnrolledAt(Instant.now());
        entityManager.persist(e);
    }

    private UsernamePasswordAuthenticationToken authToken(AppUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), null);
        return new UsernamePasswordAuthenticationToken(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
    }
}
