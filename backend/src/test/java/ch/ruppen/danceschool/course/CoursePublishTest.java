package ch.ruppen.danceschool.course;

import ch.ruppen.danceschool.TestSecurityConfig;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
class CoursePublishTest {

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
    void publish_setsPublishedAtAndReturnsOpenStatus() throws Exception {
        Course course = createDraftCourse(school, "Salsa Beginners", LocalDate.now().plusDays(30));
        entityManager.flush();

        mockMvc.perform(post("/api/courses/{id}/publish", course.getId())
                        .with(authentication(authToken(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(course.getId()))
                .andExpect(jsonPath("$.title").value("Salsa Beginners"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.publishedAt").exists());
    }

    @Test
    void publish_isIdempotent_returnsSuccessWhenAlreadyPublished() throws Exception {
        Course course = createDraftCourse(school, "Salsa Beginners", LocalDate.now().plusDays(30));
        course.setPublishedAt(LocalDate.now().minusDays(5));
        entityManager.flush();

        mockMvc.perform(post("/api/courses/{id}/publish", course.getId())
                        .with(authentication(authToken(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.publishedAt").value(LocalDate.now().minusDays(5).toString()));
    }

    @Test
    void publish_returns400_whenStartDateIsInThePast() throws Exception {
        Course course = createDraftCourse(school, "Past Course", LocalDate.now().minusDays(10));
        entityManager.flush();

        mockMvc.perform(post("/api/courses/{id}/publish", course.getId())
                        .with(authentication(authToken(testUser))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Publish Validation Failed"))
                .andExpect(jsonPath("$.fieldErrors.startDate").value("must be in the future"));
    }

    @Test
    void publish_returns400_whenStartDateIsToday() throws Exception {
        Course course = createDraftCourse(school, "Today Course", LocalDate.now());
        entityManager.flush();

        mockMvc.perform(post("/api/courses/{id}/publish", course.getId())
                        .with(authentication(authToken(testUser))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.startDate").value("must be in the future"));
    }

    @Test
    void publish_returns404_forCourseInAnotherSchool() throws Exception {
        AppUser otherUser = createUser("other@example.com", "Other User", "firebase-other");
        School otherSchool = createSchoolWithOwner("Other School", otherUser);
        Course course = createDraftCourse(otherSchool, "Other Course", LocalDate.now().plusDays(30));
        entityManager.flush();

        mockMvc.perform(post("/api/courses/{id}/publish", course.getId())
                        .with(authentication(authToken(testUser))))
                .andExpect(status().isNotFound());
    }

    @Test
    void publish_returns401_whenNotAuthenticated() throws Exception {
        Course course = createDraftCourse(school, "Salsa Beginners", LocalDate.now().plusDays(30));
        entityManager.flush();

        mockMvc.perform(post("/api/courses/{id}/publish", course.getId()))
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

    private Course createDraftCourse(School s, String title, LocalDate startDate) {
        Course course = new Course();
        course.setSchool(s);
        course.setTitle(title);
        course.setDanceStyle(DanceStyle.SALSA);
        course.setLevel(CourseLevel.BEGINNER);
        course.setCourseType(CourseType.PARTNER);
        course.setStartDate(startDate);
        course.setRecurrenceType(RecurrenceType.WEEKLY);
        course.setDayOfWeek(startDate.getDayOfWeek());
        course.setNumberOfSessions(8);
        course.setEndDate(startDate.plusWeeks(7));
        course.setStartTime(LocalTime.of(19, 0));
        course.setEndTime(LocalTime.of(20, 0));
        course.setLocation("Studio A");
        course.setMaxParticipants(15);
        course.setPriceModel(PriceModel.FIXED_COURSE);
        course.setPrice(new BigDecimal("180.00"));
        // publishedAt is null → DRAFT
        entityManager.persist(course);
        return course;
    }

    private UsernamePasswordAuthenticationToken authToken(AppUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), null);
        return new UsernamePasswordAuthenticationToken(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
    }
}
