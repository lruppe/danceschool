package ch.ruppen.danceschool.payment;

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
import java.time.temporal.ChronoUnit;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    private AppUser owner;
    private AppUser owner2;
    private School school;
    private School school2;
    private Course course;
    private Student student;

    @BeforeEach
    void setUp() {
        owner = createUser("owner@example.com", "Owner", "firebase-owner");
        owner2 = createUser("owner2@example.com", "Owner 2", "firebase-owner-2");
        school = createSchoolWithOwner("Test School", owner);
        school2 = createSchoolWithOwner("Other School", owner2);

        course = createCourse(school, "Bachata Beginners", new BigDecimal("166.50"));
        student = createStudent(school, "Anna Mueller", "anna@example.com");

        entityManager.flush();
    }

    @Test
    void list_returnsOpenAndCompletedRows_partitionedByPaidAt() throws Exception {
        Instant t1 = Instant.now().minus(5, ChronoUnit.DAYS);
        Instant t2 = Instant.now().minus(3, ChronoUnit.DAYS);

        Long openId = createEnrollment(course, student, EnrollmentStatus.PENDING_PAYMENT, t1, null);
        Long paidId = createEnrollment(course, createStudent(school, "Marco", "marco@example.com"),
                EnrollmentStatus.CONFIRMED, t1, t2);
        entityManager.flush();

        mockMvc.perform(get("/api/me/payments").with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                // Default order: billingDate DESC → paid (t2) before open (t1)
                .andExpect(jsonPath("$[0].enrollmentId").value(paidId))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].studentName").value("Marco"))
                .andExpect(jsonPath("$[0].studentEmail").value("marco@example.com"))
                .andExpect(jsonPath("$[0].courseTitle").value("Bachata Beginners"))
                .andExpect(jsonPath("$[0].amount").value(166.50))
                .andExpect(jsonPath("$[1].enrollmentId").value(openId))
                .andExpect(jsonPath("$[1].status").value("OPEN"));
    }

    @Test
    void list_billingDateResolvesToPaidAtWhenSet_elseEnrolledAt() throws Exception {
        Instant enrolled = Instant.parse("2026-04-01T10:00:00Z");
        Instant paid = Instant.parse("2026-04-10T10:00:00Z");

        createEnrollment(course, student, EnrollmentStatus.PENDING_PAYMENT, enrolled, null);
        createEnrollment(course, createStudent(school, "Marco", "marco@example.com"),
                EnrollmentStatus.CONFIRMED, enrolled, paid);
        entityManager.flush();

        mockMvc.perform(get("/api/me/payments").with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                // First row (paid, sorted by billingDate=paid DESC)
                .andExpect(jsonPath("$[0].billingDate").value(paid.toString()))
                // Second row (open, billingDate=enrolledAt)
                .andExpect(jsonPath("$[1].billingDate").value(enrolled.toString()));
    }

    @Test
    void list_excludesPendingApprovalWaitlistedAndRejectedWithoutPaidAt() throws Exception {
        Instant now = Instant.now();
        Student s2 = createStudent(school, "S2", "s2@example.com");
        Student s3 = createStudent(school, "S3", "s3@example.com");
        Student s4 = createStudent(school, "S4", "s4@example.com");

        createEnrollment(course, student, EnrollmentStatus.PENDING_APPROVAL, now, null);
        createEnrollment(course, s2, EnrollmentStatus.WAITLISTED, now, null);
        createEnrollment(course, s3, EnrollmentStatus.REJECTED, now, null);
        // PENDING_PAYMENT row should be the only one returned
        Long openId = createEnrollment(course, s4, EnrollmentStatus.PENDING_PAYMENT, now, null);
        entityManager.flush();

        mockMvc.perform(get("/api/me/payments").with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].enrollmentId").value(openId))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void list_includesRejectedWithPaidAt_asCompleted() throws Exception {
        // Edge case: any row with paidAt set is COMPLETED, regardless of status — matches the
        // per-course OPEN_PAYMENTS view's semantics. Refunds aren't modeled in v1.
        Instant t = Instant.now();
        Long id = createEnrollment(course, student, EnrollmentStatus.REJECTED, t.minusSeconds(60), t);
        entityManager.flush();

        mockMvc.perform(get("/api/me/payments").with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].enrollmentId").value(id))
                .andExpect(jsonPath("$[0].status").value("COMPLETED"));
    }

    @Test
    void list_orderedByBillingDateDesc() throws Exception {
        Instant base = Instant.now();
        Student s2 = createStudent(school, "S2", "s2@example.com");
        Student s3 = createStudent(school, "S3", "s3@example.com");

        Long oldest = createEnrollment(course, student, EnrollmentStatus.PENDING_PAYMENT,
                base.minus(10, ChronoUnit.DAYS), null);
        Long middle = createEnrollment(course, s2, EnrollmentStatus.CONFIRMED,
                base.minus(8, ChronoUnit.DAYS), base.minus(5, ChronoUnit.DAYS));
        Long newest = createEnrollment(course, s3, EnrollmentStatus.PENDING_PAYMENT,
                base.minus(1, ChronoUnit.DAYS), null);
        entityManager.flush();

        mockMvc.perform(get("/api/me/payments").with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enrollmentId").value(newest))
                .andExpect(jsonPath("$[1].enrollmentId").value(middle))
                .andExpect(jsonPath("$[2].enrollmentId").value(oldest));
    }

    @Test
    void list_tenantIsolation_onlyReturnsCallersSchoolPayments() throws Exception {
        Course otherCourse = createCourse(school2, "Other School Course", new BigDecimal("99.00"));
        Student otherStudent = createStudent(school2, "Other Student", "other@example.com");

        Instant now = Instant.now();
        createEnrollment(course, student, EnrollmentStatus.PENDING_PAYMENT, now, null);
        Long otherId = createEnrollment(otherCourse, otherStudent, EnrollmentStatus.PENDING_PAYMENT, now, null);
        entityManager.flush();

        mockMvc.perform(get("/api/me/payments").with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].courseTitle").value("Bachata Beginners"));

        mockMvc.perform(get("/api/me/payments").with(authentication(authToken(owner2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].enrollmentId").value(otherId))
                .andExpect(jsonPath("$[0].courseTitle").value("Other School Course"));
    }

    // --- Helpers ---

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

    private Course createCourse(School s, String title, BigDecimal price) {
        Course c = new Course();
        c.setSchool(s);
        c.setTitle(title);
        c.setDanceStyle(DanceStyle.BACHATA);
        c.setLevel(CourseLevel.BEGINNER);
        c.setCourseType(CourseType.PARTNER);
        c.setMaxParticipants(20);
        c.setStartDate(LocalDate.now().plusWeeks(2));
        c.setEndDate(LocalDate.now().plusWeeks(10));
        c.setLocation("Studio A");
        c.setTeachers("Teacher");
        c.setStartTime(LocalTime.of(19, 0));
        c.setEndTime(LocalTime.of(20, 0));
        c.setDayOfWeek(DayOfWeek.MONDAY);
        c.setRecurrenceType(RecurrenceType.WEEKLY);
        c.setNumberOfSessions(8);
        c.setPriceModel(PriceModel.FIXED_COURSE);
        c.setPrice(price);
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

    private Long createEnrollment(Course c, Student s, EnrollmentStatus status,
                                  Instant enrolledAt, Instant paidAt) {
        Enrollment e = new Enrollment();
        e.setCourse(c);
        e.setStudent(s);
        e.setDanceRole(DanceRole.LEAD);
        e.setStatus(status);
        e.setEnrolledAt(enrolledAt);
        e.setPaidAt(paidAt);
        entityManager.persist(e);
        return e.getId();
    }

    private UsernamePasswordAuthenticationToken authToken(AppUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail());
        return new UsernamePasswordAuthenticationToken(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
    }
}
