package ch.ruppen.danceschool.enrollment;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
class WaitlistAutoPromotionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private AppUser owner;
    private School school;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        owner = createUser("owner@example.com", "Owner", "firebase-owner");
        school = createSchoolWithOwner("Test School", owner);

        logAppender = new ListAppender<>();
        logAppender.start();
        ((Logger) LoggerFactory.getLogger("business")).addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        ((Logger) LoggerFactory.getLogger("business")).detachAppender(logAppender);
    }

    // --- Direct-pay path ---

    @Test
    void enrollStudent_directPay_promotesRoleImbalanceWaitlisted_andRenumbers() throws Exception {
        Course course = createCourse("AP Direct", CourseType.PARTNER, 20, 3);
        seedSeatHolder(course, DanceRole.LEAD);
        seedSeatHolder(course, DanceRole.LEAD);
        seedSeatHolder(course, DanceRole.LEAD);
        Enrollment leadW1 = seedWaitlisted(course, DanceRole.LEAD, WaitlistReason.ROLE_IMBALANCE, 1);
        Enrollment leadW2 = seedWaitlisted(course, DanceRole.LEAD, WaitlistReason.ROLE_IMBALANCE, 2);

        Student follow = createStudent("Follow A", "followA@example.com");
        entityManager.flush();

        mockMvc.perform(post("/api/courses/{id}/enrollments", course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "FOLLOW"}
                                """.formatted(follow.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));

        entityManager.flush();
        entityManager.clear();

        Enrollment promoted = entityManager.find(Enrollment.class, leadW1.getId());
        assertThat(promoted.getStatus()).isEqualTo(EnrollmentStatus.PENDING_PAYMENT);
        assertThat(promoted.getWaitlistReason()).isNull();
        assertThat(promoted.getWaitlistPosition()).isNull();

        Enrollment stillWaitlisted = entityManager.find(Enrollment.class, leadW2.getId());
        assertThat(stillWaitlisted.getStatus()).isEqualTo(EnrollmentStatus.WAITLISTED);
        assertThat(stillWaitlisted.getWaitlistReason()).isEqualTo(WaitlistReason.ROLE_IMBALANCE);
        assertThat(stillWaitlisted.getWaitlistPosition()).isEqualTo(1);

        assertBusinessEventFired("EnrollmentAutoPromoted", 1);
    }

    // --- Approval path ---

    @Test
    void approveEnrollment_promotesRoleImbalanceWaitlisted_andRenumbers() throws Exception {
        // ADVANCED course so an under-leveled FOLLOW routes to PENDING_APPROVAL first,
        // then approval triggers auto-promote.
        Course course = createCourse("AP Approve", CourseType.PARTNER, 20, 3,
                DanceStyle.SALSA, CourseLevel.ADVANCED);
        seedSeatHolder(course, DanceRole.LEAD, DanceStyle.SALSA, CourseLevel.ADVANCED);
        seedSeatHolder(course, DanceRole.LEAD, DanceStyle.SALSA, CourseLevel.ADVANCED);
        seedSeatHolder(course, DanceRole.LEAD, DanceStyle.SALSA, CourseLevel.ADVANCED);
        Enrollment leadW1 = seedWaitlisted(course, DanceRole.LEAD, WaitlistReason.ROLE_IMBALANCE, 1);
        Enrollment leadW2 = seedWaitlisted(course, DanceRole.LEAD, WaitlistReason.ROLE_IMBALANCE, 2);

        Student underLeveledFollow = createStudent("P Follow", "pfollow@example.com");
        entityManager.flush();

        String resp = mockMvc.perform(post("/api/courses/{id}/enrollments", course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "FOLLOW"}
                                """.formatted(underLeveledFollow.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andReturn().getResponse().getContentAsString();
        Long followId = com.jayway.jsonpath.JsonPath.parse(resp).read("$.enrollmentId", Long.class);

        mockMvc.perform(put("/api/enrollments/{id}/approve", followId)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));

        entityManager.flush();
        entityManager.clear();

        Enrollment promoted = entityManager.find(Enrollment.class, leadW1.getId());
        assertThat(promoted.getStatus()).isEqualTo(EnrollmentStatus.PENDING_PAYMENT);
        assertThat(promoted.getWaitlistReason()).isNull();
        assertThat(promoted.getWaitlistPosition()).isNull();

        Enrollment stillWaitlisted = entityManager.find(Enrollment.class, leadW2.getId());
        assertThat(stillWaitlisted.getStatus()).isEqualTo(EnrollmentStatus.WAITLISTED);
        assertThat(stillWaitlisted.getWaitlistPosition()).isEqualTo(1);

        assertBusinessEventFired("EnrollmentAutoPromoted", 1);
    }

    // --- Trigger guard: no promotion when capacity is full ---

    @Test
    void approve_whenCourseFull_doesNotPromote() throws Exception {
        Course course = createCourse("AP Full", CourseType.PARTNER, 4, 3,
                DanceStyle.SALSA, CourseLevel.ADVANCED);
        seedSeatHolder(course, DanceRole.LEAD, DanceStyle.SALSA, CourseLevel.ADVANCED);
        seedSeatHolder(course, DanceRole.LEAD, DanceStyle.SALSA, CourseLevel.ADVANCED);
        seedSeatHolder(course, DanceRole.LEAD, DanceStyle.SALSA, CourseLevel.ADVANCED);
        seedSeatHolder(course, DanceRole.FOLLOW, DanceStyle.SALSA, CourseLevel.ADVANCED);
        Enrollment leadW = seedWaitlisted(course, DanceRole.LEAD, WaitlistReason.ROLE_IMBALANCE, 1);

        // Under-leveled FOLLOW applies → PENDING_APPROVAL. Approving while course is full
        // routes this follow to WAITLISTED(CAPACITY); trigger does not fire.
        Student extraFollow = createStudent("Extra Follow", "extraFollow@example.com");
        entityManager.flush();

        String resp = mockMvc.perform(post("/api/courses/{id}/enrollments", course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "FOLLOW"}
                                """.formatted(extraFollow.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andReturn().getResponse().getContentAsString();
        Long id = com.jayway.jsonpath.JsonPath.parse(resp).read("$.enrollmentId", Long.class);

        mockMvc.perform(put("/api/enrollments/{id}/approve", id)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITLISTED"));

        entityManager.flush();
        entityManager.clear();

        Enrollment untouched = entityManager.find(Enrollment.class, leadW.getId());
        assertThat(untouched.getStatus()).isEqualTo(EnrollmentStatus.WAITLISTED);
        assertThat(untouched.getWaitlistReason()).isEqualTo(WaitlistReason.ROLE_IMBALANCE);
        assertThat(untouched.getWaitlistPosition()).isEqualTo(1);

        assertBusinessEventFired("EnrollmentAutoPromoted", 0);
    }

    // --- Same-role waitlisted not promoted ---

    @Test
    void enrollStudent_sameRoleArrival_doesNotPromoteSameRoleWaitlisted() throws Exception {
        Course course = createCourse("AP SameRole", CourseType.PARTNER, 20, 1);
        seedSeatHolder(course, DanceRole.LEAD);
        Enrollment waitlistedLead = seedWaitlisted(course, DanceRole.LEAD, WaitlistReason.ROLE_IMBALANCE, 1);

        // Extra LEAD joins while threshold is 1: 1L+0F → adding LEAD makes 2L+0F within threshold
        // (diff 2 > 1 → WAITLISTED). So status = WAITLISTED, trigger does not fire.
        Student extraLead = createStudent("Extra Lead", "extraLead@example.com");
        entityManager.flush();

        mockMvc.perform(post("/api/courses/{id}/enrollments", course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "LEAD"}
                                """.formatted(extraLead.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITLISTED"));

        entityManager.flush();
        entityManager.clear();

        Enrollment stillWaitlisted = entityManager.find(Enrollment.class, waitlistedLead.getId());
        assertThat(stillWaitlisted.getStatus()).isEqualTo(EnrollmentStatus.WAITLISTED);
        assertBusinessEventFired("EnrollmentAutoPromoted", 0);
    }

    // --- Capacity-waitlisted not promoted ---

    @Test
    void enrollStudent_capacityWaitlisted_isNotPromotedByOppositeRoleJoin() throws Exception {
        // Course at capacity: a CAPACITY-waitlisted entry cannot be promoted because no seat is free.
        // The short-circuit in autoPromoteWaitlist guarantees this even if a seat-holding enrollment
        // could somehow land (it cannot here; the new arrival itself waitlists with CAPACITY).
        Course course = createCourse("AP Capacity", CourseType.PARTNER, 2, null);
        seedSeatHolder(course, DanceRole.LEAD);
        seedSeatHolder(course, DanceRole.FOLLOW);
        Enrollment capacityLead = seedWaitlisted(course, DanceRole.LEAD, WaitlistReason.CAPACITY, 1);

        Student extraFollow = createStudent("Extra Follow", "extraFollow@example.com");
        entityManager.flush();

        mockMvc.perform(post("/api/courses/{id}/enrollments", course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "FOLLOW"}
                                """.formatted(extraFollow.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("WAITLISTED"));

        entityManager.flush();
        entityManager.clear();

        Enrollment untouched = entityManager.find(Enrollment.class, capacityLead.getId());
        assertThat(untouched.getStatus()).isEqualTo(EnrollmentStatus.WAITLISTED);
        assertThat(untouched.getWaitlistReason()).isEqualTo(WaitlistReason.CAPACITY);
        assertBusinessEventFired("EnrollmentAutoPromoted", 0);
    }

    // --- Solo and no-threshold courses: trigger fires but resolveWaitlist short-circuits ---

    @Test
    void enrollStudent_soloCourse_doesNotPromoteWithoutWaitlist() throws Exception {
        // Solo course with no waitlisted entries — trigger fires, loop iterates nothing, no log.
        Course course = createCourse("AP Solo", CourseType.SOLO, 20, null,
                DanceStyle.SALSA, CourseLevel.BEGINNER);
        Student s = createStudent("Solo Student", "solo@example.com");
        entityManager.flush();

        mockMvc.perform(post("/api/courses/{id}/enrollments", course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d}
                                """.formatted(s.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));

        assertBusinessEventFired("EnrollmentAutoPromoted", 0);
    }

    @Test
    void enrollStudent_partnerCourseWithNullThreshold_doesNotPromote() throws Exception {
        // Null threshold means balancing is off, so no entry is ever naturally waitlisted with
        // ROLE_IMBALANCE. Trigger fires, loop finds no candidates, nothing is promoted.
        Course course = createCourse("AP NoThreshold", CourseType.PARTNER, 20, null);
        seedSeatHolder(course, DanceRole.LEAD);

        Student follow = createStudent("Follow", "followNT@example.com");
        entityManager.flush();

        mockMvc.perform(post("/api/courses/{id}/enrollments", course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "FOLLOW"}
                                """.formatted(follow.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));

        assertBusinessEventFired("EnrollmentAutoPromoted", 0);
    }

    // --- Chained promotion ---

    @Test
    void enrollStudent_chainedPromotion_promotesMultipleInOneTrigger() throws Exception {
        // Constructed via direct seeding: 2 FOLLOWs + 0 LEADs committed with 2 LEAD entries
        // waitlisted under ROLE_IMBALANCE. This state is artificial (no natural path creates
        // it without a cancel flow), but it verifies the loop handles chained promotions.
        // A new FOLLOW triggers auto-promote (3F + 0L within threshold=3); both waitlisted
        // LEADs then resolve to null (0L, 1L are within 3F+3) and promote in sequence.
        Course course = createCourse("AP Chain", CourseType.PARTNER, 20, 3);
        seedSeatHolder(course, DanceRole.FOLLOW);
        seedSeatHolder(course, DanceRole.FOLLOW);
        Enrollment leadW1 = seedWaitlisted(course, DanceRole.LEAD, WaitlistReason.ROLE_IMBALANCE, 1);
        Enrollment leadW2 = seedWaitlisted(course, DanceRole.LEAD, WaitlistReason.ROLE_IMBALANCE, 2);

        Student follow = createStudent("New Follow", "chainFollow@example.com");
        entityManager.flush();

        mockMvc.perform(post("/api/courses/{id}/enrollments", course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "FOLLOW"}
                                """.formatted(follow.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_PAYMENT"));

        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(Enrollment.class, leadW1.getId()).getStatus())
                .isEqualTo(EnrollmentStatus.PENDING_PAYMENT);
        assertThat(entityManager.find(Enrollment.class, leadW2.getId()).getStatus())
                .isEqualTo(EnrollmentStatus.PENDING_PAYMENT);

        assertBusinessEventFired("EnrollmentAutoPromoted", 2);
    }

    // --- Query budget ---

    @Test
    void enrollStudent_noPromotionCandidates_doesNotScanWaitlistBeyondBaseline() throws Exception {
        // With no waitlisted entries, autoPromoteWaitlist runs 1 capacity-count query + 1 listing
        // query + the renumber listing (only if a promotion happened — it doesn't here).
        Course course = createCourse("AP NoCandidates", CourseType.SOLO, 20, null,
                DanceStyle.SALSA, CourseLevel.BEGINNER);
        Student s = createStudent("Counter Student", "counter@example.com");
        entityManager.flush();
        entityManager.clear();

        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        mockMvc.perform(post("/api/courses/{id}/enrollments", course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d}
                                """.formatted(s.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated());

        // Baseline enrollment work + 2 auto-promote queries (capacity count + empty waitlist listing).
        assertThat(stats.getPrepareStatementCount())
                .as("SQL budget for enroll with no waitlist candidates")
                .isLessThanOrEqualTo(15);
    }

    @Test
    void enrollStudent_onePromotionCandidate_staysWithinQueryBudget() throws Exception {
        // Mirrors the 0-candidate budget test but with one promotion triggered: the delta is the
        // resolveWaitlist recheck, the promote UPDATE, and the renumber listing.
        Course course = createCourse("AP Budget1", CourseType.PARTNER, 20, 3);
        seedSeatHolder(course, DanceRole.LEAD);
        seedSeatHolder(course, DanceRole.LEAD);
        seedSeatHolder(course, DanceRole.LEAD);
        seedWaitlisted(course, DanceRole.LEAD, WaitlistReason.ROLE_IMBALANCE, 1);

        Student follow = createStudent("Budget Follow", "budgetFollow@example.com");
        entityManager.flush();
        entityManager.clear();

        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        stats.clear();

        mockMvc.perform(post("/api/courses/{id}/enrollments", course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"studentId": %d, "danceRole": "FOLLOW"}
                                """.formatted(follow.getId()))
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated());

        assertThat(stats.getPrepareStatementCount())
                .as("SQL budget for enroll with one promotion")
                .isLessThanOrEqualTo(25);
    }

    // --- Helpers ---

    private void assertBusinessEventFired(String event, int expectedCount) {
        long count = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(m -> m.startsWith("event=" + event + " ") || m.equals("event=" + event))
                .count();
        assertThat(count).as("count of '%s' business logs", event).isEqualTo(expectedCount);
    }

    private Course createCourse(String title, CourseType type, int maxParticipants, Integer threshold) {
        return createCourse(title, type, maxParticipants, threshold, DanceStyle.BACHATA, CourseLevel.BEGINNER);
    }

    private Course createCourse(String title, CourseType type, int maxParticipants, Integer threshold,
                                DanceStyle style, CourseLevel level) {
        Course c = new Course();
        c.setSchool(school);
        c.setTitle(title);
        c.setDanceStyle(style);
        c.setLevel(level);
        c.setCourseType(type);
        c.setMaxParticipants(maxParticipants);
        c.setRoleBalanceThreshold(threshold);
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

    private Student seedSeatHolder(Course course, DanceRole role) {
        return seedSeatHolder(course, role, null, null);
    }

    private Student seedSeatHolder(Course course, DanceRole role, DanceStyle danceLevelStyle, CourseLevel danceLevel) {
        Student s = createStudent("SeatHolder " + java.util.UUID.randomUUID(),
                "seat-" + java.util.UUID.randomUUID() + "@example.com");
        if (danceLevelStyle != null) {
            addDanceLevel(s, danceLevelStyle, danceLevel);
        }
        Enrollment e = new Enrollment();
        e.setCourse(course);
        e.setStudent(s);
        e.setDanceRole(role);
        e.setStatus(EnrollmentStatus.PENDING_PAYMENT);
        e.setEnrolledAt(Instant.now());
        entityManager.persist(e);
        return s;
    }

    private Enrollment seedWaitlisted(Course course, DanceRole role, WaitlistReason reason, int position) {
        Student s = createStudent("Waitlisted " + java.util.UUID.randomUUID(),
                "wl-" + java.util.UUID.randomUUID() + "@example.com");
        Enrollment e = new Enrollment();
        e.setCourse(course);
        e.setStudent(s);
        e.setDanceRole(role);
        e.setStatus(EnrollmentStatus.WAITLISTED);
        e.setWaitlistReason(reason);
        e.setWaitlistPosition(position);
        e.setEnrolledAt(Instant.now());
        entityManager.persist(e);
        return e;
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

    private Student createStudent(String name, String email) {
        Student st = new Student();
        st.setSchool(school);
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

    private UsernamePasswordAuthenticationToken authToken(AppUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), null);
        return new UsernamePasswordAuthenticationToken(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
    }
}
