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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
class EnrollmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    private AppUser owner;
    private School school;
    private Course partnerCourse;
    private Course soloCourse;
    private Student student;

    @BeforeEach
    void setUp() {
        owner = createUser("owner@example.com", "Owner", "firebase-owner");
        school = createSchoolWithOwner("Test School", owner);

        partnerCourse = createCourse(school, "Bachata Intermediate", DanceStyle.BACHATA,
                CourseLevel.INTERMEDIATE, CourseType.PARTNER, 15, true, 3);

        soloCourse = createCourse(school, "Salsa Solo Beginner", DanceStyle.SALSA,
                CourseLevel.BEGINNER, CourseType.SOLO, 12, false, null);

        student = createStudent(school, "Anna Mueller", "anna@example.com", "+41 79 100 0001");
        addDanceLevel(student, DanceStyle.BACHATA, CourseLevel.INTERMEDIATE);
        addDanceLevel(student, DanceStyle.SALSA, CourseLevel.BEGINNER);

        entityManager.flush();
    }

    // --- Enroll student ---

    @Test
    void enrollStudent_partnerCourse_returnsCreatedWithPendingPayment() throws Exception {
        mockMvc.perform(post("/api/courses/{id}/enrollments", partnerCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "LEAD"}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enrollmentId").isNumber())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
    }

    @Test
    void enrollStudent_soloCourse_noDanceRole_returnsCreated() throws Exception {
        mockMvc.perform(post("/api/courses/{id}/enrollments", soloCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
    }

    @Test
    void enrollStudent_partnerCourse_missingDanceRole_returns409() throws Exception {
        mockMvc.perform(post("/api/courses/{id}/enrollments", partnerCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isConflict());
    }

    @Test
    void enrollStudent_soloCourse_withDanceRole_returns409() throws Exception {
        mockMvc.perform(post("/api/courses/{id}/enrollments", soloCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "LEAD"}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isConflict());
    }

    @Test
    void enrollStudent_duplicate_returns409() throws Exception {
        // First enrollment succeeds
        mockMvc.perform(post("/api/courses/{id}/enrollments", soloCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated());

        // Second enrollment for same student/course fails
        mockMvc.perform(post("/api/courses/{id}/enrollments", soloCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isConflict());
    }

    @Test
    void enrollStudent_atCapacity_returnsWaitlisted_withCapacityReason_andFifoPosition() throws Exception {
        Course tinyCourse = createCourse(school, "Tiny Course", DanceStyle.SALSA,
                CourseLevel.BEGINNER, CourseType.SOLO, 1, false, null);
        entityManager.flush();

        Student student2 = createStudent(school, "Marco Rossi", "marco@example.com", null);
        Student student3 = createStudent(school, "Laura Weber", "laura@example.com", null);
        entityManager.flush();

        // Fill the one spot
        mockMvc.perform(post("/api/courses/{id}/enrollments", tinyCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));

        // Second student goes to waitlist with CAPACITY reason and position 1
        String resp2 = mockMvc.perform(post("/api/courses/{id}/enrollments", tinyCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d}
                                """.formatted(student2.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITLISTED"))
                .andReturn().getResponse().getContentAsString();

        Long waitlistedId = com.jayway.jsonpath.JsonPath.parse(resp2).read("$.enrollmentId", Long.class);
        entityManager.flush();
        entityManager.clear();
        Enrollment waitlisted = entityManager.find(Enrollment.class, waitlistedId);
        org.junit.jupiter.api.Assertions.assertEquals(WaitlistReason.CAPACITY, waitlisted.getWaitlistReason());
        org.junit.jupiter.api.Assertions.assertEquals(1, waitlisted.getWaitlistPosition());

        // Third student: waitlist position 2
        String resp3 = mockMvc.perform(post("/api/courses/{id}/enrollments", tinyCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d}
                                """.formatted(student3.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITLISTED"))
                .andReturn().getResponse().getContentAsString();

        Long waitlistedId2 = com.jayway.jsonpath.JsonPath.parse(resp3).read("$.enrollmentId", Long.class);
        entityManager.flush();
        entityManager.clear();
        Enrollment waitlisted2 = entityManager.find(Enrollment.class, waitlistedId2);
        org.junit.jupiter.api.Assertions.assertEquals(2, waitlisted2.getWaitlistPosition());
    }

    @Test
    void enrollStudent_atCapacity_doesNotIncrementEnrolledStudentsCounter() throws Exception {
        Course tinyCourse = createCourse(school, "Tiny Course", DanceStyle.SALSA,
                CourseLevel.BEGINNER, CourseType.SOLO, 1, false, null);
        entityManager.flush();

        Student student2 = createStudent(school, "Marco Rossi", "marco@example.com", null);
        entityManager.flush();

        mockMvc.perform(post("/api/courses/{id}/enrollments", tinyCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/courses/{id}/enrollments", tinyCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d}
                                """.formatted(student2.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITLISTED"));

        entityManager.flush();
        entityManager.clear();

        Course refreshed = entityManager.find(Course.class, tinyCourse.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, refreshed.getEnrolledStudents());
    }

    @Test
    void enrollStudent_roleImbalance_returnsWaitlisted_withRoleImbalanceReason() throws Exception {
        // threshold=1 → tolerate up to a 1-diff. With 1 LEAD + 0 FOLLOW (diff=1=threshold, fine),
        // a second LEAD would make diff=2 > threshold → waitlist with ROLE_IMBALANCE.
        Course balancedCourse = createCourse(school, "Kizomba Balanced", DanceStyle.KIZOMBA,
                CourseLevel.BEGINNER, CourseType.PARTNER, 20, true, 1);
        entityManager.flush();

        Student leadA = createStudent(school, "Lead A", "leada@example.com", null);
        Student leadB = createStudent(school, "Lead B", "leadb@example.com", null);
        entityManager.flush();

        enrollPartner(balancedCourse.getId(), leadA.getId(), "LEAD", "PENDING_PAYMENT");

        String resp = enrollPartner(balancedCourse.getId(), leadB.getId(), "LEAD", "WAITLISTED");
        Long id = com.jayway.jsonpath.JsonPath.parse(resp).read("$.enrollmentId", Long.class);
        entityManager.flush();
        entityManager.clear();
        Enrollment waitlisted = entityManager.find(Enrollment.class, id);
        org.junit.jupiter.api.Assertions.assertEquals(WaitlistReason.ROLE_IMBALANCE, waitlisted.getWaitlistReason());
        org.junit.jupiter.api.Assertions.assertEquals(1, waitlisted.getWaitlistPosition());
    }

    @Test
    void enrollStudent_roleImbalance_onlyCountsActiveStatuses() throws Exception {
        // threshold=1, 1 LEAD (REJECTED — excluded) + 1 FOLLOW; next LEAD should NOT waitlist because
        // rejected enrollments don't count. It's PENDING_PAYMENT.
        Course advancedCourse = createCourse(school, "Salsa Advanced Gate", DanceStyle.SALSA,
                CourseLevel.ADVANCED, CourseType.PARTNER, 20, true, 1);
        entityManager.flush();

        Student rejectedLead = createStudent(school, "Rejected Lead", "rejlead@example.com", null);
        Student follow = createStudent(school, "Follow", "follow@example.com", null);
        Student newLead = createStudent(school, "New Lead", "newlead@example.com", null);
        addDanceLevel(follow, DanceStyle.SALSA, CourseLevel.ADVANCED);
        addDanceLevel(newLead, DanceStyle.SALSA, CourseLevel.ADVANCED);
        entityManager.flush();

        String respRejected = enrollPartner(advancedCourse.getId(), rejectedLead.getId(), "LEAD", "PENDING_APPROVAL");
        Long rejectedId = com.jayway.jsonpath.JsonPath.parse(respRejected).read("$.enrollmentId", Long.class);
        mockMvc.perform(put("/api/enrollments/{id}/reject", rejectedId)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isOk());

        enrollPartner(advancedCourse.getId(), follow.getId(), "FOLLOW", "PENDING_PAYMENT");
        // Rejected LEAD no longer counts; 0 active LEAD + 1 FOLLOW, adding 1 LEAD → 1 vs 1, diff 0, not waitlisted
        enrollPartner(advancedCourse.getId(), newLead.getId(), "LEAD", "PENDING_PAYMENT");
    }

    @Test
    void enrollStudent_waitlistPosition_perRoleFIFO() throws Exception {
        // tinyCourse max=2: enroll 1 LEAD + 1 FOLLOW to fill capacity, then waitlist additional students.
        // Positions should be assigned per role: LEAD #1, LEAD #2, FOLLOW #1.
        Course tinyPartner = createCourse(school, "Kizomba Tiny", DanceStyle.KIZOMBA,
                CourseLevel.BEGINNER, CourseType.PARTNER, 2, false, null);
        entityManager.flush();

        Student leadA = createStudent(school, "Lead A", "leada2@example.com", null);
        Student followA = createStudent(school, "Follow A", "followa2@example.com", null);
        Student leadB = createStudent(school, "Lead B", "leadb2@example.com", null);
        Student leadC = createStudent(school, "Lead C", "leadc2@example.com", null);
        Student followB = createStudent(school, "Follow B", "followb2@example.com", null);
        entityManager.flush();

        enrollPartner(tinyPartner.getId(), leadA.getId(), "LEAD", "PENDING_PAYMENT");
        enrollPartner(tinyPartner.getId(), followA.getId(), "FOLLOW", "PENDING_PAYMENT");
        // Now at capacity — remaining enrollments go to the waitlist with CAPACITY reason.

        String respLeadB = enrollPartner(tinyPartner.getId(), leadB.getId(), "LEAD", "WAITLISTED");
        String respLeadC = enrollPartner(tinyPartner.getId(), leadC.getId(), "LEAD", "WAITLISTED");
        String respFollowB = enrollPartner(tinyPartner.getId(), followB.getId(), "FOLLOW", "WAITLISTED");

        entityManager.flush();
        entityManager.clear();

        Enrollment lB = entityManager.find(Enrollment.class,
                com.jayway.jsonpath.JsonPath.parse(respLeadB).read("$.enrollmentId", Long.class));
        Enrollment lC = entityManager.find(Enrollment.class,
                com.jayway.jsonpath.JsonPath.parse(respLeadC).read("$.enrollmentId", Long.class));
        Enrollment fB = entityManager.find(Enrollment.class,
                com.jayway.jsonpath.JsonPath.parse(respFollowB).read("$.enrollmentId", Long.class));

        org.junit.jupiter.api.Assertions.assertEquals(1, lB.getWaitlistPosition());
        org.junit.jupiter.api.Assertions.assertEquals(2, lC.getWaitlistPosition());
        org.junit.jupiter.api.Assertions.assertEquals(1, fB.getWaitlistPosition());
    }

    private String enrollPartner(Long courseId, Long studentId, String role, String expectedStatus) throws Exception {
        return mockMvc.perform(post("/api/courses/{id}/enrollments", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "%s"}
                                """.formatted(studentId, role))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void enrollStudent_insufficientLevel_returnsPendingApproval() throws Exception {
        Course advancedCourse = createCourse(school, "Salsa Advanced", DanceStyle.SALSA,
                CourseLevel.ADVANCED, CourseType.PARTNER, 10, true, 2);
        entityManager.flush();

        // Student has BEGINNER salsa level, course requires ADVANCED → needs approval
        mockMvc.perform(post("/api/courses/{id}/enrollments", advancedCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "LEAD"}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
    }

    @Test
    void enrollStudent_noLevelForStyle_returnsPendingApproval() throws Exception {
        Course intermediateZouk = createCourse(school, "Zouk Intermediate", DanceStyle.ZOUK,
                CourseLevel.INTERMEDIATE, CourseType.PARTNER, 10, false, null);
        entityManager.flush();

        // Student has no ZOUK level at all → needs approval
        mockMvc.perform(post("/api/courses/{id}/enrollments", intermediateZouk.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "FOLLOW"}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
    }

    @Test
    void enrollStudent_requiresApprovalFlag_returnsPendingApproval_evenWithMatchingLevel() throws Exception {
        Course approvalCourse = createCourseWithApproval(school, "Bachata Approval", DanceStyle.BACHATA,
                CourseLevel.INTERMEDIATE, CourseType.PARTNER, 10, true);
        entityManager.flush();

        // Student has INTERMEDIATE bachata (matches course level), but requiresApproval=true
        mockMvc.perform(post("/api/courses/{id}/enrollments", approvalCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "LEAD"}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));
    }

    @Test
    void enrollStudent_beginnerCourseWithRequiresApproval_bypassesApproval() throws Exception {
        // Even with requiresApproval=true, BEGINNER courses skip approval
        Course approvalBeginner = createCourseWithApproval(school, "Kizomba Beginner Approval",
                DanceStyle.KIZOMBA, CourseLevel.BEGINNER, CourseType.PARTNER, 10, true);
        entityManager.flush();

        mockMvc.perform(post("/api/courses/{id}/enrollments", approvalBeginner.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "FOLLOW"}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
    }

    @Test
    void enrollStudent_beginnerCourse_alwaysAllowed() throws Exception {
        // Student with no Kizomba level can still enroll in BEGINNER Kizomba
        Course beginnerKizomba = createCourse(school, "Kizomba Beginner", DanceStyle.KIZOMBA,
                CourseLevel.BEGINNER, CourseType.PARTNER, 10, false, null);
        entityManager.flush();

        mockMvc.perform(post("/api/courses/{id}/enrollments", beginnerKizomba.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "FOLLOW"}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
    }

    // --- List enrollments ---

    @Test
    void listEnrollments_returnsAllEnrollmentsForCourse() throws Exception {
        Student student2 = createStudent(school, "Marco Rossi", "marco@example.com", null);
        addDanceLevel(student2, DanceStyle.SALSA, CourseLevel.BEGINNER);
        entityManager.flush();

        mockMvc.perform(post("/api/courses/{id}/enrollments", soloCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/courses/{id}/enrollments", soloCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d}
                                """.formatted(student2.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/courses/{id}/enrollments", soloCourse.getId())
                        .with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].studentName").isString())
                .andExpect(jsonPath("$[0].status").value("PENDING_PAYMENT"));
    }

    // --- Mark paid ---

    @Test
    void markPaid_transitionsToConfirmed() throws Exception {
        String response = mockMvc.perform(post("/api/courses/{id}/enrollments", soloCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long enrollmentId = com.jayway.jsonpath.JsonPath.parse(response).read("$.enrollmentId", Long.class);

        mockMvc.perform(put("/api/enrollments/{id}/mark-paid", enrollmentId)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void markPaid_alreadyConfirmed_returns409() throws Exception {
        String response = mockMvc.perform(post("/api/courses/{id}/enrollments", soloCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long enrollmentId = com.jayway.jsonpath.JsonPath.parse(response).read("$.enrollmentId", Long.class);

        // First mark-paid succeeds
        mockMvc.perform(put("/api/enrollments/{id}/mark-paid", enrollmentId)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isOk());

        // Second mark-paid fails (already CONFIRMED)
        mockMvc.perform(put("/api/enrollments/{id}/mark-paid", enrollmentId)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isConflict());
    }

    // --- Approve / reject ---

    @Test
    void enroll_withPendingApprovalApplicants_doesNotBlockCommittedEnrollment() throws Exception {
        // Course with capacity 2, no requiresApproval, ADVANCED level.
        Course advancedCourse = createCourse(school, "Salsa Advanced", DanceStyle.SALSA,
                CourseLevel.ADVANCED, CourseType.PARTNER, 2, true, 1);
        entityManager.flush();

        // student (BEGINNER salsa) → PENDING_APPROVAL; should NOT reserve a seat.
        mockMvc.perform(post("/api/courses/{id}/enrollments", advancedCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "LEAD"}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"));

        // Two matching-level students can still take both direct-pay seats.
        Student qualified1 = createStudent(school, "Q1", "q1@example.com", null);
        addDanceLevel(qualified1, DanceStyle.SALSA, CourseLevel.ADVANCED);
        Student qualified2 = createStudent(school, "Q2", "q2@example.com", null);
        addDanceLevel(qualified2, DanceStyle.SALSA, CourseLevel.ADVANCED);
        entityManager.flush();

        mockMvc.perform(post("/api/courses/{id}/enrollments", advancedCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "FOLLOW"}
                                """.formatted(qualified1.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));

        mockMvc.perform(post("/api/courses/{id}/enrollments", advancedCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "LEAD"}
                                """.formatted(qualified2.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));
    }

    @Test
    void approve_whenCourseFull_routesToWaitlistWithCapacityReason() throws Exception {
        // Course capacity 1, ADVANCED salsa.
        Course advancedCourse = createCourse(school, "Salsa Advanced", DanceStyle.SALSA,
                CourseLevel.ADVANCED, CourseType.PARTNER, 1, true, 1);
        entityManager.flush();

        // Fill the one seat with a qualified direct-pay student.
        Student qualified = createStudent(school, "Qualified", "qualified@example.com", null);
        addDanceLevel(qualified, DanceStyle.SALSA, CourseLevel.ADVANCED);
        entityManager.flush();

        mockMvc.perform(post("/api/courses/{id}/enrollments", advancedCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "LEAD"}
                                """.formatted(qualified.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));

        // Under-leveled student enrolls → PENDING_APPROVAL.
        String pendingResponse = mockMvc.perform(post("/api/courses/{id}/enrollments", advancedCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "FOLLOW"}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andReturn().getResponse().getContentAsString();
        Long pendingId = com.jayway.jsonpath.JsonPath.parse(pendingResponse).read("$.enrollmentId", Long.class);

        // Approve: course is at capacity → WAITLISTED with reason CAPACITY.
        mockMvc.perform(put("/api/enrollments/{id}/approve", pendingId)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITLISTED"));

        entityManager.flush();
        entityManager.clear();

        Enrollment updated = entityManager.find(Enrollment.class, pendingId);
        org.junit.jupiter.api.Assertions.assertEquals(EnrollmentStatus.WAITLISTED, updated.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(WaitlistReason.CAPACITY, updated.getWaitlistReason());
        // approvedAt is still set — approval decision is separate from slot outcome.
        org.junit.jupiter.api.Assertions.assertNotNull(updated.getApprovedAt());
    }

    @Test
    void approve_transitionsToPendingPayment_setsApprovedAt_andUpgradesStudentLevel() throws Exception {
        Long enrollmentId = createPendingApprovalForInsufficientLevel();

        mockMvc.perform(put("/api/enrollments/{id}/approve", enrollmentId)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));

        entityManager.flush();
        entityManager.clear();

        Enrollment updated = entityManager.find(Enrollment.class, enrollmentId);
        org.junit.jupiter.api.Assertions.assertNotNull(updated.getApprovedAt());

        // Student had BEGINNER Salsa; approving an ADVANCED Salsa course should upgrade to ADVANCED
        Student refreshed = entityManager.find(Student.class, student.getId());
        CourseLevel salsaLevel = refreshed.getDanceLevels().stream()
                .filter(dl -> dl.getDanceStyle() == DanceStyle.SALSA)
                .map(StudentDanceLevel::getLevel)
                .findFirst()
                .orElse(null);
        org.junit.jupiter.api.Assertions.assertEquals(CourseLevel.ADVANCED, salsaLevel);
    }

    @Test
    void approve_doesNotDowngradeExistingHigherDanceLevel() throws Exception {
        // Course is INTERMEDIATE bachata with requiresApproval=true; student has INTERMEDIATE bachata
        // But to trigger PENDING_APPROVAL we need requiresApproval; and to test non-downgrade we use ADVANCED student
        Student advancedStudent = createStudent(school, "Laura Advanced", "laura@example.com", null);
        addDanceLevel(advancedStudent, DanceStyle.BACHATA, CourseLevel.ADVANCED);
        Course approvalCourse = createCourseWithApproval(school, "Bachata Approval", DanceStyle.BACHATA,
                CourseLevel.INTERMEDIATE, CourseType.PARTNER, 10, true);
        entityManager.flush();

        String response = mockMvc.perform(post("/api/courses/{id}/enrollments", approvalCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "LEAD"}
                                """.formatted(advancedStudent.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long enrollmentId = com.jayway.jsonpath.JsonPath.parse(response).read("$.enrollmentId", Long.class);

        mockMvc.perform(put("/api/enrollments/{id}/approve", enrollmentId)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isOk());

        entityManager.flush();
        entityManager.clear();

        Student refreshed = entityManager.find(Student.class, advancedStudent.getId());
        CourseLevel bachataLevel = refreshed.getDanceLevels().stream()
                .filter(dl -> dl.getDanceStyle() == DanceStyle.BACHATA)
                .map(StudentDanceLevel::getLevel)
                .findFirst()
                .orElse(null);
        org.junit.jupiter.api.Assertions.assertEquals(CourseLevel.ADVANCED, bachataLevel);
    }

    @Test
    void reject_transitionsToRejected() throws Exception {
        Long enrollmentId = createPendingApprovalForInsufficientLevel();

        mockMvc.perform(put("/api/enrollments/{id}/reject", enrollmentId)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void approve_onNonPendingApproval_returns409() throws Exception {
        // soloCourse is BEGINNER → goes directly to PENDING_PAYMENT
        String response = mockMvc.perform(post("/api/courses/{id}/enrollments", soloCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long enrollmentId = com.jayway.jsonpath.JsonPath.parse(response).read("$.enrollmentId", Long.class);

        mockMvc.perform(put("/api/enrollments/{id}/approve", enrollmentId)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isConflict());
    }

    @Test
    void reject_onNonPendingApproval_returns409() throws Exception {
        String response = mockMvc.perform(post("/api/courses/{id}/enrollments", soloCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long enrollmentId = com.jayway.jsonpath.JsonPath.parse(response).read("$.enrollmentId", Long.class);

        mockMvc.perform(put("/api/enrollments/{id}/reject", enrollmentId)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectedEnrollment_excludedFromCapacity_allowsReenrollment() throws Exception {
        Course advancedCourse = createCourse(school, "Salsa Advanced", DanceStyle.SALSA,
                CourseLevel.ADVANCED, CourseType.PARTNER, 10, true, 2);
        entityManager.flush();

        String response = mockMvc.perform(post("/api/courses/{id}/enrollments", advancedCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "LEAD"}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long firstId = com.jayway.jsonpath.JsonPath.parse(response).read("$.enrollmentId", Long.class);

        mockMvc.perform(put("/api/enrollments/{id}/reject", firstId)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isOk());

        // Re-enroll the same student — should succeed with a new enrollment
        String response2 = mockMvc.perform(post("/api/courses/{id}/enrollments", advancedCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "LEAD"}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andReturn().getResponse().getContentAsString();
        Long secondId = com.jayway.jsonpath.JsonPath.parse(response2).read("$.enrollmentId", Long.class);
        org.junit.jupiter.api.Assertions.assertNotEquals(firstId, secondId);
    }

    @Test
    void listEnrollments_includesStudentDanceLevel() throws Exception {
        Long enrollmentId = createPendingApprovalForInsufficientLevel();

        mockMvc.perform(get("/api/courses/{id}/enrollments",
                        entityManager.find(Enrollment.class, enrollmentId).getCourse().getId())
                        .with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].studentDanceLevel").value("BEGINNER"));
    }

    // --- Tenant isolation ---

    @Test
    void enrollStudent_otherOwnersCourse_returns404() throws Exception {
        AppUser owner2 = createUser("owner2@example.com", "Owner 2", "firebase-owner-2");
        School school2 = createSchoolWithOwner("Other School", owner2);
        Student student2 = createStudent(school2, "Elena Fischer", "elena@example.com", null);
        entityManager.flush();

        // Owner2 tries to enroll their student in owner1's course
        mockMvc.perform(post("/api/courses/{id}/enrollments", soloCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d}
                                """.formatted(student2.getId()))
                        .with(authentication(authToken(owner2))))
                .andExpect(status().isNotFound());
    }

    @Test
    void listEnrollments_otherOwnersCourse_returns404() throws Exception {
        AppUser owner2 = createUser("owner2@example.com", "Owner 2", "firebase-owner-2");
        createSchoolWithOwner("Other School", owner2);
        entityManager.flush();

        mockMvc.perform(get("/api/courses/{id}/enrollments", soloCourse.getId())
                        .with(authentication(authToken(owner2))))
                .andExpect(status().isNotFound());
    }

    @Test
    void markPaid_otherOwnersEnrollment_returns404() throws Exception {
        // Enroll as owner1
        String response = mockMvc.perform(post("/api/courses/{id}/enrollments", soloCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long enrollmentId = com.jayway.jsonpath.JsonPath.parse(response).read("$.enrollmentId", Long.class);

        // Owner2 tries to mark-paid
        AppUser owner2 = createUser("owner2@example.com", "Owner 2", "firebase-owner-2");
        createSchoolWithOwner("Other School", owner2);
        entityManager.flush();

        mockMvc.perform(put("/api/enrollments/{id}/mark-paid", enrollmentId)
                        .with(authentication(authToken(owner2))))
                .andExpect(status().isNotFound());
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

    private Course createCourseWithApproval(School s, String title, DanceStyle danceStyle, CourseLevel level,
                                            CourseType courseType, int maxParticipants, boolean requiresApproval) {
        Course c = createCourse(s, title, danceStyle, level, courseType, maxParticipants, false, null);
        c.setRequiresApproval(requiresApproval);
        return c;
    }

    private Long createPendingApprovalForInsufficientLevel() throws Exception {
        Course advancedCourse = createCourse(school, "Salsa Advanced", DanceStyle.SALSA,
                CourseLevel.ADVANCED, CourseType.PARTNER, 10, true, 2);
        entityManager.flush();

        String response = mockMvc.perform(post("/api/courses/{id}/enrollments", advancedCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "LEAD"}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.parse(response).read("$.enrollmentId", Long.class);
    }

    private Course createCourse(School s, String title, DanceStyle danceStyle, CourseLevel level,
                                CourseType courseType, int maxParticipants,
                                boolean roleBalancing, Integer roleBalanceThreshold) {
        Course c = new Course();
        c.setSchool(s);
        c.setTitle(title);
        c.setDanceStyle(danceStyle);
        c.setLevel(level);
        c.setCourseType(courseType);
        c.setMaxParticipants(maxParticipants);
        c.setRoleBalancingEnabled(roleBalancing);
        c.setRoleBalanceThreshold(roleBalanceThreshold);
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

    private Student createStudent(School s, String name, String email, String phoneNumber) {
        Student st = new Student();
        st.setSchool(s);
        st.setName(name);
        st.setEmail(email);
        st.setPhoneNumber(phoneNumber);
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

    private UsernamePasswordAuthenticationToken authToken(AppUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail());
        return new UsernamePasswordAuthenticationToken(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
    }
}
