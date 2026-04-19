package ch.ruppen.danceschool.course;

import ch.ruppen.danceschool.TestSecurityConfig;
import ch.ruppen.danceschool.enrollment.DanceRole;
import ch.ruppen.danceschool.enrollment.Enrollment;
import ch.ruppen.danceschool.enrollment.EnrollmentStatus;
import ch.ruppen.danceschool.school.School;
import ch.ruppen.danceschool.schoolmember.MemberRole;
import ch.ruppen.danceschool.schoolmember.SchoolMember;
import ch.ruppen.danceschool.shared.security.AuthenticatedUser;
import ch.ruppen.danceschool.student.Student;
import ch.ruppen.danceschool.user.AppUser;
import jakarta.persistence.EntityManager;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
class CourseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    private AppUser testUser;
    private School school;

    @BeforeEach
    void setUp() {
        testUser = createUser("test@example.com", "Test User", "firebase-test");
        school = createSchoolWithOwner("Test Dance School", testUser);
        entityManager.flush();
    }

    @Test
    void getMe_returnsCourses_whenUserHasCourses() throws Exception {
        Course course = createCourse(school, "Salsa Beginners", DanceStyle.SALSA, CourseLevel.BEGINNER,
                DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(20, 0),
                8, 15, new BigDecimal("180.00"), LocalDate.now());
        // Committed enrollments (PENDING_PAYMENT + CONFIRMED) count as enrolledStudents.
        // WAITLISTED / PENDING_APPROVAL must not be counted.
        createEnrollment(course, createStudent(school, "S1", "s1@example.com"), null, EnrollmentStatus.CONFIRMED);
        createEnrollment(course, createStudent(school, "S2", "s2@example.com"), null, EnrollmentStatus.CONFIRMED);
        createEnrollment(course, createStudent(school, "S3", "s3@example.com"), null, EnrollmentStatus.CONFIRMED);
        createEnrollment(course, createStudent(school, "S4", "s4@example.com"), null, EnrollmentStatus.PENDING_PAYMENT);
        createEnrollment(course, createStudent(school, "S5", "s5@example.com"), null, EnrollmentStatus.PENDING_PAYMENT);
        createEnrollment(course, createStudent(school, "S6", "s6@example.com"), null, EnrollmentStatus.WAITLISTED);
        createEnrollment(course, createStudent(school, "S7", "s7@example.com"), null, EnrollmentStatus.PENDING_APPROVAL);
        entityManager.flush();

        mockMvc.perform(get("/api/courses/me")
                        .with(authentication(authToken(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Salsa Beginners"))
                .andExpect(jsonPath("$[0].danceStyle").value("SALSA"))
                .andExpect(jsonPath("$[0].level").value("BEGINNER"))
                .andExpect(jsonPath("$[0].courseType").value("PARTNER"))
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$[0].startTime").value("19:00:00"))
                .andExpect(jsonPath("$[0].endTime").value("20:00:00"))
                .andExpect(jsonPath("$[0].numberOfSessions").value(8))
                .andExpect(jsonPath("$[0].enrolledStudents").value(5))
                .andExpect(jsonPath("$[0].leadCount").value(0))
                .andExpect(jsonPath("$[0].followCount").value(0))
                .andExpect(jsonPath("$[0].maxParticipants").value(15))
                .andExpect(jsonPath("$[0].price").value(180.00))
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[0].startDate").exists())
                .andExpect(jsonPath("$[0].completedSessions").isNumber());
    }

    @Test
    void getMe_returnsEmptyList_whenNoCourses() throws Exception {
        mockMvc.perform(get("/api/courses/me")
                        .with(authentication(authToken(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getMe_returnsMultipleCourses() throws Exception {
        createCourse(school, "Salsa Beginners", DanceStyle.SALSA, CourseLevel.BEGINNER,
                DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(20, 0),
                8, 15, new BigDecimal("180.00"), LocalDate.now());
        createCourse(school, "Bachata Advanced", DanceStyle.BACHATA, CourseLevel.ADVANCED,
                DayOfWeek.WEDNESDAY, LocalTime.of(20, 0), LocalTime.of(21, 15),
                10, 10, new BigDecimal("310.00"), LocalDate.now());
        entityManager.flush();

        mockMvc.perform(get("/api/courses/me")
                        .with(authentication(authToken(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getMe_returnsRoleCounts_fromCommittedEnrollments() throws Exception {
        Course partnerCourse = createCourseEntity(school, "Bachata Intermediate", DanceStyle.BACHATA,
                CourseLevel.INTERMEDIATE, CourseType.PARTNER,
                DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(20, 0),
                8, 20, new BigDecimal("200.00"), LocalDate.now());
        Course soloCourse = createCourseEntity(school, "Salsa Solo", DanceStyle.SALSA,
                CourseLevel.BEGINNER, CourseType.SOLO,
                DayOfWeek.TUESDAY, LocalTime.of(19, 0), LocalTime.of(20, 0),
                8, 20, new BigDecimal("180.00"), LocalDate.now());

        Student s1 = createStudent(school, "Anna", "anna@example.com");
        Student s2 = createStudent(school, "Ben", "ben@example.com");
        Student s3 = createStudent(school, "Cara", "cara@example.com");
        Student s4 = createStudent(school, "Dan", "dan@example.com");
        Student s5 = createStudent(school, "Eva", "eva@example.com");

        // Partner course: 2 leads CONFIRMED + 1 lead PENDING_PAYMENT + 1 follow PENDING_PAYMENT
        createEnrollment(partnerCourse, s1, DanceRole.LEAD, EnrollmentStatus.CONFIRMED);
        createEnrollment(partnerCourse, s2, DanceRole.LEAD, EnrollmentStatus.CONFIRMED);
        createEnrollment(partnerCourse, s3, DanceRole.LEAD, EnrollmentStatus.PENDING_PAYMENT);
        createEnrollment(partnerCourse, s4, DanceRole.FOLLOW, EnrollmentStatus.PENDING_PAYMENT);
        // WAITLISTED + PENDING_APPROVAL must not be counted
        createEnrollment(partnerCourse, s5, DanceRole.FOLLOW, EnrollmentStatus.WAITLISTED);

        entityManager.flush();

        mockMvc.perform(get("/api/courses/me")
                        .with(authentication(authToken(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.id == " + partnerCourse.getId() + ")].leadCount").value(3))
                .andExpect(jsonPath("$[?(@.id == " + partnerCourse.getId() + ")].followCount").value(1))
                .andExpect(jsonPath("$[?(@.id == " + partnerCourse.getId() + ")].courseType").value("PARTNER"))
                .andExpect(jsonPath("$[?(@.id == " + soloCourse.getId() + ")].leadCount").value(0))
                .andExpect(jsonPath("$[?(@.id == " + soloCourse.getId() + ")].followCount").value(0))
                .andExpect(jsonPath("$[?(@.id == " + soloCourse.getId() + ")].courseType").value("SOLO"));
    }

    @Test
    void getMe_returns404_whenUserHasNoSchool() throws Exception {
        AppUser orphan = createUser("orphan@example.com", "Orphan", "firebase-orphan");
        entityManager.flush();

        mockMvc.perform(get("/api/courses/me")
                        .with(authentication(authToken(orphan))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMe_returnsUnauthorized_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/courses/me"))
                .andExpect(status().isUnauthorized());
    }

    private AppUser createUser(String email, String name, String firebaseUid) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setName(name);
        user.setFirebaseUid(firebaseUid);
        entityManager.persist(user);
        return user;
    }

    private School createSchoolWithOwner(String name, AppUser owner) {
        School s = new School();
        s.setName(name);
        entityManager.persist(s);

        SchoolMember member = new SchoolMember();
        member.setUser(owner);
        member.setSchool(s);
        member.setRole(MemberRole.OWNER);
        entityManager.persist(member);

        return s;
    }

    private Course createCourse(School s, String title, DanceStyle danceStyle, CourseLevel level,
                                DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                                int sessions, int max, BigDecimal price,
                                LocalDate publishedAt) {
        return createCourseEntity(s, title, danceStyle, level, CourseType.PARTNER,
                dayOfWeek, startTime, endTime, sessions, max, price, publishedAt);
    }

    private Course createCourseEntity(School s, String title, DanceStyle danceStyle, CourseLevel level,
                                      CourseType courseType, DayOfWeek dayOfWeek, LocalTime startTime,
                                      LocalTime endTime, int sessions, int max,
                                      BigDecimal price, LocalDate publishedAt) {
        LocalDate startDate = LocalDate.now().plusDays(30);
        Course course = new Course();
        course.setSchool(s);
        course.setTitle(title);
        course.setDanceStyle(danceStyle);
        course.setLevel(level);
        course.setCourseType(courseType);
        course.setStartDate(startDate);
        course.setRecurrenceType(RecurrenceType.WEEKLY);
        course.setDayOfWeek(dayOfWeek);
        course.setNumberOfSessions(sessions);
        course.setEndDate(startDate.plusWeeks(sessions - 1));
        course.setStartTime(startTime);
        course.setEndTime(endTime);
        course.setLocation("Studio A");
        course.setMaxParticipants(max);
        course.setPriceModel(PriceModel.FIXED_COURSE);
        course.setPrice(price);
        course.setPublishedAt(publishedAt);
        entityManager.persist(course);
        return course;
    }

    private Student createStudent(School s, String name, String email) {
        Student student = new Student();
        student.setSchool(s);
        student.setName(name);
        student.setEmail(email);
        entityManager.persist(student);
        return student;
    }

    private void createEnrollment(Course course, Student student, DanceRole role, EnrollmentStatus status) {
        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setStudent(student);
        enrollment.setDanceRole(role);
        enrollment.setStatus(status);
        enrollment.setEnrolledAt(Instant.now());
        entityManager.persist(enrollment);
    }

    private UsernamePasswordAuthenticationToken authToken(AppUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail());
        return new UsernamePasswordAuthenticationToken(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
    }
}
