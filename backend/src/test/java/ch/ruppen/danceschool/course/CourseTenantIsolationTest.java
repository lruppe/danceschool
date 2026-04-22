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
class CourseTenantIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    private AppUser userA;
    private AppUser userB;
    private School schoolA;
    private School schoolB;

    @BeforeEach
    void setUp() {
        userA = createUser("user-a@example.com", "User A", "firebase-a");
        userB = createUser("user-b@example.com", "User B", "firebase-b");

        schoolA = createSchoolWithOwner("School A", userA);
        schoolB = createSchoolWithOwner("School B", userB);

        createCourse(schoolA, "School A - Salsa");
        createCourse(schoolA, "School A - Bachata");
        createCourse(schoolB, "School B - Salsa");

        entityManager.flush();
    }

    @Test
    void getMe_returnsOnlyOwnSchoolCourses() throws Exception {
        mockMvc.perform(get("/api/courses/me")
                        .with(authentication(authToken(userA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("School A - Salsa"))
                .andExpect(jsonPath("$[1].title").value("School A - Bachata"));

        mockMvc.perform(get("/api/courses/me")
                        .with(authentication(authToken(userB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("School B - Salsa"));
    }

    @Test
    void getMe_coOwnerSeesSchoolCourses() throws Exception {
        AppUser coOwner = createUser("co-owner@example.com", "Co-Owner", "firebase-co");
        addOwner(schoolA, coOwner);
        entityManager.flush();

        mockMvc.perform(get("/api/courses/me")
                        .with(authentication(authToken(coOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getMe_returns403_forUserWithNoSchool() throws Exception {
        AppUser orphan = createUser("orphan@example.com", "Orphan", "firebase-orphan");
        entityManager.flush();

        mockMvc.perform(get("/api/courses/me")
                        .with(authentication(authToken(orphan))))
                .andExpect(status().isForbidden());
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
        addOwner(school, owner);
        return school;
    }

    private void addOwner(School school, AppUser owner) {
        SchoolMember member = new SchoolMember();
        member.setUser(owner);
        member.setSchool(school);
        member.setRole(MemberRole.OWNER);
        entityManager.persist(member);
    }

    private void createCourse(School school, String title) {
        LocalDate startDate = LocalDate.of(2026, 4, 7);
        Course course = new Course();
        course.setSchool(school);
        course.setTitle(title);
        course.setDanceStyle(DanceStyle.SALSA);
        course.setLevel(CourseLevel.BEGINNER);
        course.setCourseType(CourseType.PARTNER);
        course.setStartDate(startDate);
        course.setRecurrenceType(RecurrenceType.WEEKLY);
        course.setDayOfWeek(DayOfWeek.MONDAY);
        course.setNumberOfSessions(8);
        course.setEndDate(startDate.plusWeeks(7));
        course.setStartTime(LocalTime.of(19, 0));
        course.setEndTime(LocalTime.of(20, 0));
        course.setLocation("Studio A");
        course.setMaxParticipants(15);
        course.setPriceModel(PriceModel.FIXED_COURSE);
        course.setPrice(new BigDecimal("180.00"));
        course.setPublishedAt(LocalDate.of(2026, 3, 1));
        entityManager.persist(course);
    }

    private UsernamePasswordAuthenticationToken authToken(AppUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), null);
        return new UsernamePasswordAuthenticationToken(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
    }
}
