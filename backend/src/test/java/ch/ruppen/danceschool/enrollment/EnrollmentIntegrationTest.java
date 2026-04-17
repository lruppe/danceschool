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
    void enrollStudent_atCapacity_returns409() throws Exception {
        Course tinyCourse = createCourse(school, "Tiny Course", DanceStyle.SALSA,
                CourseLevel.BEGINNER, CourseType.SOLO, 1, false, null);
        entityManager.flush();

        Student student2 = createStudent(school, "Marco Rossi", "marco@example.com", null);
        entityManager.flush();

        // Fill the one spot
        mockMvc.perform(post("/api/courses/{id}/enrollments", tinyCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated());

        // Second student cannot enroll
        mockMvc.perform(post("/api/courses/{id}/enrollments", tinyCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d}
                                """.formatted(student2.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isConflict());
    }

    @Test
    void enrollStudent_insufficientLevel_returns409() throws Exception {
        Course advancedCourse = createCourse(school, "Salsa Advanced", DanceStyle.SALSA,
                CourseLevel.ADVANCED, CourseType.PARTNER, 10, true, 2);
        entityManager.flush();

        // Student has BEGINNER salsa level, course requires ADVANCED
        mockMvc.perform(post("/api/courses/{id}/enrollments", advancedCourse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "LEAD"}
                                """.formatted(student.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isConflict());
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
