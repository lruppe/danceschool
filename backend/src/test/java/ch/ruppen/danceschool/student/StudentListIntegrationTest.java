package ch.ruppen.danceschool.student;

import ch.ruppen.danceschool.TestSecurityConfig;
import ch.ruppen.danceschool.course.Course;
import ch.ruppen.danceschool.course.CourseLevel;
import ch.ruppen.danceschool.course.CourseType;
import ch.ruppen.danceschool.course.DanceStyle;
import ch.ruppen.danceschool.course.PriceModel;
import ch.ruppen.danceschool.course.RecurrenceType;
import ch.ruppen.danceschool.enrollment.DanceRole;
import ch.ruppen.danceschool.enrollment.Enrollment;
import ch.ruppen.danceschool.enrollment.EnrollmentStatus;
import ch.ruppen.danceschool.school.School;
import ch.ruppen.danceschool.schoolmember.MemberRole;
import ch.ruppen.danceschool.schoolmember.SchoolMember;
import ch.ruppen.danceschool.shared.security.AuthenticatedUser;
import ch.ruppen.danceschool.user.AppUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
class StudentListIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private AppUser ownerA;
    private AppUser ownerB;
    private School schoolA;
    private School schoolB;

    @BeforeEach
    void setUp() {
        ownerA = createUser("owner-a@example.com", "Owner A", "firebase-a");
        ownerB = createUser("owner-b@example.com", "Owner B", "firebase-b");
        schoolA = createSchoolWithOwner("School A", ownerA);
        schoolB = createSchoolWithOwner("School B", ownerB);
    }

    @Test
    void list_scopesToCallerSchool() throws Exception {
        createStudent(schoolA, "Alice", "alice@example.com");
        createStudent(schoolA, "Bob", "bob@example.com");
        createStudent(schoolB, "Carol", "carol@example.com");
        entityManager.flush();

        mockMvc.perform(get("/api/students")
                        .with(authentication(authToken(ownerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Alice"))
                .andExpect(jsonPath("$[1].name").value("Bob"));

        mockMvc.perform(get("/api/students")
                        .with(authentication(authToken(ownerB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Carol"));
    }

    @Test
    void list_returnsEmptyList_forSchoolWithoutStudents() throws Exception {
        entityManager.flush();

        mockMvc.perform(get("/api/students")
                        .with(authentication(authToken(ownerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void list_countsSeatHoldingEnrollmentsInPublishedNotFinishedCoursesOnly() throws Exception {
        LocalDate today = LocalDate.now();

        Course running = createCourse(schoolA, "Running",
                today.minusDays(1), today.minusWeeks(3), today.plusWeeks(3));
        Course open = createCourse(schoolA, "Open",
                today.minusDays(1), today.plusDays(7), today.plusWeeks(8));
        Course finished = createCourse(schoolA, "Finished",
                today.minusMonths(3), today.minusMonths(2), today.minusDays(2));
        Course draft = createCourse(schoolA, "Draft",
                null, today.plusDays(10), today.plusWeeks(10));

        Student zeroActive = createStudent(schoolA, "Zero Active", "zero@example.com");
        Student oneActive = createStudent(schoolA, "One Active", "one@example.com");
        Student twoActive = createStudent(schoolA, "Two Active", "two@example.com");

        // zeroActive: only non-counting courses, or non-seat-holding statuses on a counted course
        createEnrollment(finished, zeroActive, EnrollmentStatus.CONFIRMED);
        createEnrollment(draft, zeroActive, EnrollmentStatus.CONFIRMED);
        createEnrollment(running, zeroActive, EnrollmentStatus.WAITLISTED);
        createEnrollment(running, zeroActive, EnrollmentStatus.REJECTED);
        createEnrollment(running, zeroActive, EnrollmentStatus.PENDING_APPROVAL);

        // oneActive: a single seat-holding enrollment in an OPEN course (now counts)
        createEnrollment(open, oneActive, EnrollmentStatus.CONFIRMED);
        // plus noise that must not count
        createEnrollment(finished, oneActive, EnrollmentStatus.CONFIRMED);
        createEnrollment(running, oneActive, EnrollmentStatus.WAITLISTED);

        // twoActive: seat-holding in one RUNNING and one OPEN course (one CONFIRMED, one PENDING_PAYMENT).
        // Plus a duplicate CONFIRMED enrollment on the RUNNING course — DISTINCT collapses it to 1.
        createEnrollment(running, twoActive, EnrollmentStatus.CONFIRMED);
        createEnrollment(running, twoActive, EnrollmentStatus.CONFIRMED);
        createEnrollment(open, twoActive, EnrollmentStatus.PENDING_PAYMENT);

        entityManager.flush();

        mockMvc.perform(get("/api/students")
                        .with(authentication(authToken(ownerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                // Ordered by name ASC
                .andExpect(jsonPath("$[0].name").value("One Active"))
                .andExpect(jsonPath("$[0].activeCoursesCount").value(1))
                .andExpect(jsonPath("$[1].name").value("Two Active"))
                .andExpect(jsonPath("$[1].activeCoursesCount").value(2))
                .andExpect(jsonPath("$[2].name").value("Zero Active"))
                .andExpect(jsonPath("$[2].activeCoursesCount").value(0));
    }

    @Test
    void list_firesExactlyOneQueryForTheListItself() throws Exception {
        for (int i = 0; i < 10; i++) {
            Student s = createStudent(schoolA, "Student " + i, "s" + i + "@example.com");
            Course c = createCourse(schoolA, "Course " + i,
                    LocalDate.now().minusDays(1),
                    LocalDate.now().minusWeeks(1),
                    LocalDate.now().plusWeeks(3));
            createEnrollment(c, s, EnrollmentStatus.CONFIRMED);
        }
        entityManager.flush();
        entityManager.clear();

        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        mockMvc.perform(get("/api/students")
                        .with(authentication(authToken(ownerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)));

        // The list itself fires 1 query (aggregate over student + enrollment + course).
        // Budget also covers the auth user lookup + school-membership lookup.
        assertThat(stats.getPrepareStatementCount())
                .as("SQL budget for /api/students")
                .isLessThanOrEqualTo(3);
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
        School school = new School();
        school.setName(name);
        entityManager.persist(school);

        SchoolMember member = new SchoolMember();
        member.setUser(ownerUser);
        member.setSchool(school);
        member.setRole(MemberRole.OWNER);
        entityManager.persist(member);

        return school;
    }

    private Student createStudent(School school, String name, String email) {
        Student student = new Student();
        student.setSchool(school);
        student.setName(name);
        student.setEmail(email);
        entityManager.persist(student);
        return student;
    }

    private Course createCourse(School school, String title, LocalDate publishedAt,
                                LocalDate startDate, LocalDate endDate) {
        Course c = new Course();
        c.setSchool(school);
        c.setTitle(title);
        c.setDanceStyle(DanceStyle.SALSA);
        c.setLevel(CourseLevel.BEGINNER);
        c.setCourseType(CourseType.PARTNER);
        c.setMaxParticipants(20);
        c.setStartDate(startDate);
        c.setEndDate(endDate);
        c.setLocation("Studio A");
        c.setTeachers("Test Teacher");
        c.setStartTime(LocalTime.of(19, 0));
        c.setEndTime(LocalTime.of(20, 0));
        c.setDayOfWeek(DayOfWeek.MONDAY);
        c.setRecurrenceType(RecurrenceType.WEEKLY);
        c.setNumberOfSessions(8);
        c.setPriceModel(PriceModel.FIXED_COURSE);
        c.setPrice(new BigDecimal("200.00"));
        c.setPublishedAt(publishedAt);
        entityManager.persist(c);
        return c;
    }

    private void createEnrollment(Course course, Student student, EnrollmentStatus status) {
        Enrollment e = new Enrollment();
        e.setCourse(course);
        e.setStudent(student);
        e.setDanceRole(DanceRole.LEAD);
        e.setStatus(status);
        e.setEnrolledAt(Instant.now());
        entityManager.persist(e);
    }

    private UsernamePasswordAuthenticationToken authToken(AppUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), null);
        return new UsernamePasswordAuthenticationToken(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
    }
}
