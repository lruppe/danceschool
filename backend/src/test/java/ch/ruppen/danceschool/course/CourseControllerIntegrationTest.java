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
        createCourse(school, "Salsa Beginners", DanceStyle.SALSA, CourseLevel.BEGINNER,
                DayOfWeek.MONDAY, LocalTime.of(19, 0), LocalTime.of(20, 0),
                8, 5, 15, new BigDecimal("180.00"), CourseStatus.ACTIVE);
        entityManager.flush();

        mockMvc.perform(get("/api/courses/me")
                        .with(authentication(authToken(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Salsa Beginners"))
                .andExpect(jsonPath("$[0].danceStyle").value("SALSA"))
                .andExpect(jsonPath("$[0].level").value("BEGINNER"))
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$[0].startTime").value("19:00:00"))
                .andExpect(jsonPath("$[0].endTime").value("20:00:00"))
                .andExpect(jsonPath("$[0].numberOfSessions").value(8))
                .andExpect(jsonPath("$[0].enrolledStudents").value(5))
                .andExpect(jsonPath("$[0].maxParticipants").value(15))
                .andExpect(jsonPath("$[0].price").value(180.00))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
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
                8, 5, 15, new BigDecimal("180.00"), CourseStatus.ACTIVE);
        createCourse(school, "Bachata Advanced", DanceStyle.BACHATA, CourseLevel.ADVANCED,
                DayOfWeek.WEDNESDAY, LocalTime.of(20, 0), LocalTime.of(21, 15),
                10, 10, 10, new BigDecimal("310.00"), CourseStatus.FULL);
        entityManager.flush();

        mockMvc.perform(get("/api/courses/me")
                        .with(authentication(authToken(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
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

    private void createCourse(School s, String title, DanceStyle danceStyle, CourseLevel level,
                              DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
                              int sessions, int enrolled, int max, BigDecimal price, CourseStatus status) {
        Course course = new Course();
        course.setSchool(s);
        course.setTitle(title);
        course.setDanceStyle(danceStyle);
        course.setLevel(level);
        course.setCourseType(CourseType.PARTNER);
        course.setStartDate(LocalDate.of(2026, 4, 7));
        course.setRecurrenceType(RecurrenceType.WEEKLY);
        course.setDayOfWeek(dayOfWeek);
        course.setNumberOfSessions(sessions);
        course.setStartTime(startTime);
        course.setEndTime(endTime);
        course.setLocation("Studio A");
        course.setMaxParticipants(max);
        course.setEnrolledStudents(enrolled);
        course.setPriceModel(PriceModel.FIXED_COURSE);
        course.setPrice(price);
        course.setStatus(status);
        entityManager.persist(course);
    }

    private UsernamePasswordAuthenticationToken authToken(AppUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail());
        return new UsernamePasswordAuthenticationToken(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
    }
}
