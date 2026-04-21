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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
class CourseFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

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

        LocalDate today = LocalDate.now();

        // DRAFT: published_at is null, future start
        createCourse(schoolA, "Draft Course", null,
                today.plusDays(30), today.plusDays(30 + 7 * 7));

        // OPEN: published, start in future
        createCourse(schoolA, "Open Course", today.minusDays(5),
                today.plusDays(14), today.plusDays(14 + 7 * 7));

        // RUNNING: published, start in past, end in future
        createCourse(schoolA, "Running Course", today.minusDays(30),
                today.minusDays(7), today.plusDays(49));

        // FINISHED: end date in past
        createCourse(schoolA, "Finished Course", today.minusDays(90),
                today.minusDays(60), today.minusDays(1));

        // School B course — should never appear in School A queries
        createCourse(schoolB, "Other School Course", today.minusDays(5),
                today.plusDays(14), today.plusDays(14 + 7 * 7));

        entityManager.flush();
    }

    @Test
    void noFilter_returnsActiveCoursesExcludingFinished() throws Exception {
        mockMvc.perform(get("/api/courses/me")
                        .with(authentication(authToken(ownerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.status=='FINISHED')]").isEmpty());
    }

    @Test
    void filterDraft_returnsOnlyUnpublished() throws Exception {
        mockMvc.perform(get("/api/courses/me?status=DRAFT")
                        .with(authentication(authToken(ownerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Draft Course"))
                .andExpect(jsonPath("$[0].status").value("DRAFT"));
    }

    @Test
    void filterOpen_returnsPublishedWithFutureStart() throws Exception {
        mockMvc.perform(get("/api/courses/me?status=OPEN")
                        .with(authentication(authToken(ownerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Open Course"))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    void filterRunning_returnsPublishedWithCurrentDates() throws Exception {
        mockMvc.perform(get("/api/courses/me?status=RUNNING")
                        .with(authentication(authToken(ownerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Running Course"))
                .andExpect(jsonPath("$[0].status").value("RUNNING"));
    }

    @Test
    void filterFinished_returnsPastEndDate() throws Exception {
        mockMvc.perform(get("/api/courses/me?status=FINISHED")
                        .with(authentication(authToken(ownerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Finished Course"))
                .andExpect(jsonPath("$[0].status").value("FINISHED"));
    }

    @Test
    void filter_respectsTenantIsolation() throws Exception {
        mockMvc.perform(get("/api/courses/me?status=OPEN")
                        .with(authentication(authToken(ownerB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Other School Course"));
    }

    @Test
    void dtoIncludesStartDate() throws Exception {
        mockMvc.perform(get("/api/courses/me?status=RUNNING")
                        .with(authentication(authToken(ownerA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startDate").exists());
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
        School school = new School();
        school.setName(name);
        entityManager.persist(school);

        SchoolMember member = new SchoolMember();
        member.setUser(owner);
        member.setSchool(school);
        member.setRole(MemberRole.OWNER);
        entityManager.persist(member);

        return school;
    }

    private void createCourse(School school, String title, LocalDate publishedAt,
                              LocalDate startDate, LocalDate endDate) {
        Course course = new Course();
        course.setSchool(school);
        course.setTitle(title);
        course.setDanceStyle(DanceStyle.SALSA);
        course.setLevel(CourseLevel.BEGINNER);
        course.setCourseType(CourseType.PARTNER);
        course.setStartDate(startDate);
        course.setRecurrenceType(RecurrenceType.WEEKLY);
        course.setDayOfWeek(startDate.getDayOfWeek());
        course.setNumberOfSessions(8);
        course.setEndDate(endDate);
        course.setStartTime(LocalTime.of(19, 0));
        course.setEndTime(LocalTime.of(20, 0));
        course.setLocation("Studio A");
        course.setMaxParticipants(15);
        course.setPriceModel(PriceModel.FIXED_COURSE);
        course.setPrice(new BigDecimal("180.00"));
        course.setPublishedAt(publishedAt);
        entityManager.persist(course);
    }

    private UsernamePasswordAuthenticationToken authToken(AppUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), null);
        return new UsernamePasswordAuthenticationToken(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
    }
}
