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
import java.time.Instant;
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
class StudentCrudIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    private AppUser owner;

    @BeforeEach
    void setUp() {
        owner = createUser("owner@example.com", "Owner", "firebase-owner");
        createSchoolWithOwner("Test School", owner);
        entityManager.flush();
    }

    @Test
    void createStudent_returnsId() throws Exception {
        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Anna Müller",
                                    "email": "anna@example.com",
                                    "phoneNumber": "+41 79 100 0001",
                                    "danceLevels": [
                                        {"danceStyle": "SALSA", "level": "INTERMEDIATE"},
                                        {"danceStyle": "BACHATA", "level": "BEGINNER"}
                                    ]
                                }
                                """)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void createStudent_withoutDanceLevels_returnsId() throws Exception {
        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "David Kim",
                                    "email": "david@example.com"
                                }
                                """)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void createStudent_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Bad Email",
                                    "email": "not-an-email"
                                }
                                """)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createStudent_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@example.com"
                                }
                                """)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStudent_returnsProfileWithDanceLevels() throws Exception {
        Student student = createStudent("Anna Müller", "anna@example.com", "+41 79 100 0001");
        addDanceLevel(student, "SALSA", "INTERMEDIATE");
        addDanceLevel(student, "BACHATA", "BEGINNER");
        entityManager.flush();

        mockMvc.perform(get("/api/students/{id}", student.getId())
                        .with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(student.getId()))
                .andExpect(jsonPath("$.name").value("Anna Müller"))
                .andExpect(jsonPath("$.email").value("anna@example.com"))
                .andExpect(jsonPath("$.phoneNumber").value("+41 79 100 0001"))
                .andExpect(jsonPath("$.danceLevels.length()").value(2));
    }

    @Test
    void getStudent_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/students/{id}", 9999)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateDanceLevels_replacesAllLevels() throws Exception {
        Student student = createStudent("Anna Müller", "anna@example.com", null);
        addDanceLevel(student, "SALSA", "BEGINNER");
        entityManager.flush();

        mockMvc.perform(put("/api/students/{id}/dance-levels", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "danceLevels": [
                                        {"danceStyle": "SALSA", "level": "ADVANCED"},
                                        {"danceStyle": "KIZOMBA", "level": "STARTER"}
                                    ]
                                }
                                """)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.student.danceLevels.length()").value(2))
                .andExpect(jsonPath("$.student.danceLevels[?(@.danceStyle=='SALSA')].level").value("ADVANCED"))
                .andExpect(jsonPath("$.student.danceLevels[?(@.danceStyle=='KIZOMBA')].level").value("STARTER"))
                .andExpect(jsonPath("$.autoConfirmedCount").value(0));
    }

    @Test
    void updateDanceLevels_clearAll() throws Exception {
        Student student = createStudent("Anna Müller", "anna@example.com", null);
        addDanceLevel(student, "SALSA", "BEGINNER");
        entityManager.flush();

        mockMvc.perform(put("/api/students/{id}/dance-levels", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "danceLevels": []
                                }
                                """)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.student.danceLevels.length()").value(0))
                .andExpect(jsonPath("$.autoConfirmedCount").value(0));
    }

    @Test
    void updateDanceLevels_keepsUnchangedStylesAndAppendsNew() throws Exception {
        Student student = createStudent("Anna Müller", "anna@example.com", null);
        addDanceLevel(student, "SALSA", "INTERMEDIATE");
        addDanceLevel(student, "BACHATA", "BEGINNER");
        entityManager.flush();

        mockMvc.perform(put("/api/students/{id}/dance-levels", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "danceLevels": [
                                        {"danceStyle": "SALSA", "level": "INTERMEDIATE"},
                                        {"danceStyle": "BACHATA", "level": "BEGINNER"},
                                        {"danceStyle": "KIZOMBA", "level": "STARTER"}
                                    ]
                                }
                                """)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.student.danceLevels.length()").value(3))
                .andExpect(jsonPath("$.student.danceLevels[?(@.danceStyle=='SALSA')].level").value("INTERMEDIATE"))
                .andExpect(jsonPath("$.student.danceLevels[?(@.danceStyle=='BACHATA')].level").value("BEGINNER"))
                .andExpect(jsonPath("$.student.danceLevels[?(@.danceStyle=='KIZOMBA')].level").value("STARTER"))
                .andExpect(jsonPath("$.autoConfirmedCount").value(0));
    }

    @Test
    void updateDanceLevels_notFound_returns404() throws Exception {
        mockMvc.perform(put("/api/students/{id}/dance-levels", 9999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "danceLevels": []
                                }
                                """)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateDanceLevels_raisingLevel_autoConfirmsPendingApproval() throws Exception {
        Student student = createStudent("Anna Müller", "anna@example.com", null);
        addDanceLevel(student, "SALSA", "BEGINNER");
        Course advancedSalsa = createCourse("Salsa Advanced", DanceStyle.SALSA, CourseLevel.ADVANCED);
        Enrollment pending = createPendingApprovalEnrollment(student, advancedSalsa, DanceRole.LEAD);
        entityManager.flush();

        mockMvc.perform(put("/api/students/{id}/dance-levels", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "danceLevels": [
                                        {"danceStyle": "SALSA", "level": "ADVANCED"}
                                    ]
                                }
                                """)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autoConfirmedCount").value(1));

        entityManager.refresh(pending);
        org.junit.jupiter.api.Assertions.assertEquals(
                EnrollmentStatus.PENDING_PAYMENT, pending.getStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(pending.getApprovedAt());
    }

    @Test
    void updateDanceLevels_loweringLevel_leavesEnrollmentsUnchanged() throws Exception {
        Student student = createStudent("Anna Müller", "anna@example.com", null);
        addDanceLevel(student, "SALSA", "INTERMEDIATE");
        Course advancedSalsa = createCourse("Salsa Advanced", DanceStyle.SALSA, CourseLevel.ADVANCED);
        Enrollment pending = createPendingApprovalEnrollment(student, advancedSalsa, DanceRole.LEAD);
        entityManager.flush();

        mockMvc.perform(put("/api/students/{id}/dance-levels", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "danceLevels": [
                                        {"danceStyle": "SALSA", "level": "BEGINNER"}
                                    ]
                                }
                                """)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autoConfirmedCount").value(0));

        entityManager.refresh(pending);
        org.junit.jupiter.api.Assertions.assertEquals(
                EnrollmentStatus.PENDING_APPROVAL, pending.getStatus());
        org.junit.jupiter.api.Assertions.assertNull(pending.getApprovedAt());
    }

    @Test
    void updateDanceLevels_onlyTouchedStylesReEvaluated() throws Exception {
        Student student = createStudent("Anna Müller", "anna@example.com", null);
        addDanceLevel(student, "SALSA", "BEGINNER");
        addDanceLevel(student, "BACHATA", "BEGINNER");
        Course salsaAdvanced = createCourse("Salsa Advanced", DanceStyle.SALSA, CourseLevel.ADVANCED);
        Course bachataAdvanced = createCourse("Bachata Advanced", DanceStyle.BACHATA, CourseLevel.ADVANCED);
        Enrollment salsaPending = createPendingApprovalEnrollment(student, salsaAdvanced, DanceRole.LEAD);
        Enrollment bachataPending = createPendingApprovalEnrollment(student, bachataAdvanced, DanceRole.LEAD);
        entityManager.flush();

        // Raising only SALSA must flip exactly the Salsa enrollment; the Bachata pending
        // stays untouched even though both started from identical BEGINNER levels.
        mockMvc.perform(put("/api/students/{id}/dance-levels", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "danceLevels": [
                                        {"danceStyle": "SALSA", "level": "ADVANCED"},
                                        {"danceStyle": "BACHATA", "level": "BEGINNER"}
                                    ]
                                }
                                """)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autoConfirmedCount").value(1));

        entityManager.refresh(salsaPending);
        entityManager.refresh(bachataPending);
        org.junit.jupiter.api.Assertions.assertEquals(
                EnrollmentStatus.PENDING_PAYMENT, salsaPending.getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(
                EnrollmentStatus.PENDING_APPROVAL, bachataPending.getStatus());
    }

    @Test
    void updateDanceLevels_approveRoutedToWaitlist_doesNotCount() throws Exception {
        Student student = createStudent("Anna Müller", "anna@example.com", null);
        addDanceLevel(student, "SALSA", "BEGINNER");
        Course fullCourse = createCourse("Salsa Advanced", DanceStyle.SALSA, CourseLevel.ADVANCED);
        fullCourse.setMaxParticipants(1);
        // Seat the only slot, so approving the pending enrollment will route to WAITLISTED
        // (CAPACITY) rather than PENDING_PAYMENT — that must not count as auto-confirmed.
        Student other = createStudent("Bob", "bob@example.com", null);
        Enrollment seat = new Enrollment();
        seat.setStudent(other);
        seat.setCourse(fullCourse);
        seat.setDanceRole(DanceRole.FOLLOW);
        seat.setStatus(EnrollmentStatus.CONFIRMED);
        seat.setEnrolledAt(Instant.now());
        entityManager.persist(seat);
        Enrollment pending = createPendingApprovalEnrollment(student, fullCourse, DanceRole.LEAD);
        entityManager.flush();

        mockMvc.perform(put("/api/students/{id}/dance-levels", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "danceLevels": [
                                        {"danceStyle": "SALSA", "level": "ADVANCED"}
                                    ]
                                }
                                """)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.autoConfirmedCount").value(0));

        entityManager.flush();
        entityManager.refresh(pending);
        org.junit.jupiter.api.Assertions.assertEquals(
                EnrollmentStatus.WAITLISTED, pending.getStatus());
    }

    @Test
    void updateDanceLevels_crossTenant_returns404() throws Exception {
        AppUser otherOwner = createUser("other@example.com", "Other", "firebase-other");
        School otherSchool = createSchoolWithOwner("Other School", otherOwner);
        Student otherStudent = new Student();
        otherStudent.setSchool(otherSchool);
        otherStudent.setName("Not Yours");
        otherStudent.setEmail("notyours@example.com");
        entityManager.persist(otherStudent);
        entityManager.flush();

        mockMvc.perform(put("/api/students/{id}/dance-levels", otherStudent.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "danceLevels": [
                                        {"danceStyle": "SALSA", "level": "ADVANCED"}
                                    ]
                                }
                                """)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isNotFound());
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

    private Student createStudent(String name, String email, String phoneNumber) {
        School school = entityManager.createQuery(
                        "SELECT s FROM School s JOIN SchoolMember m ON m.school = s WHERE m.user = :user", School.class)
                .setParameter("user", owner)
                .getSingleResult();

        Student student = new Student();
        student.setSchool(school);
        student.setName(name);
        student.setEmail(email);
        student.setPhoneNumber(phoneNumber);
        entityManager.persist(student);
        return student;
    }

    private void addDanceLevel(Student student, String style, String level) {
        StudentDanceLevel dl = new StudentDanceLevel();
        dl.setStudent(student);
        dl.setDanceStyle(DanceStyle.valueOf(style));
        dl.setLevel(CourseLevel.valueOf(level));
        student.getDanceLevels().add(dl);
        entityManager.persist(dl);
    }

    private Course createCourse(String title, DanceStyle danceStyle, CourseLevel level) {
        School school = entityManager.createQuery(
                        "SELECT s FROM School s JOIN SchoolMember m ON m.school = s WHERE m.user = :user", School.class)
                .setParameter("user", owner)
                .getSingleResult();

        Course c = new Course();
        c.setSchool(school);
        c.setTitle(title);
        c.setDanceStyle(danceStyle);
        c.setLevel(level);
        c.setCourseType(CourseType.PARTNER);
        c.setMaxParticipants(10);
        c.setRoleBalanceThreshold(3);
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

    private Enrollment createPendingApprovalEnrollment(Student student, Course course, DanceRole role) {
        Enrollment e = new Enrollment();
        e.setStudent(student);
        e.setCourse(course);
        e.setDanceRole(role);
        e.setStatus(EnrollmentStatus.PENDING_APPROVAL);
        e.setEnrolledAt(Instant.now());
        entityManager.persist(e);
        return e;
    }

    private UsernamePasswordAuthenticationToken authToken(AppUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), null);
        return new UsernamePasswordAuthenticationToken(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
    }
}
