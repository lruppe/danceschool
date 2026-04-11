package ch.ruppen.danceschool.course;

import ch.ruppen.danceschool.TestSecurityConfig;
import ch.ruppen.danceschool.school.School;
import ch.ruppen.danceschool.schoolmember.MemberRole;
import ch.ruppen.danceschool.schoolmember.SchoolMember;
import ch.ruppen.danceschool.shared.security.AuthenticatedUser;
import ch.ruppen.danceschool.user.AppUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
class CourseCrudIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ObjectMapper objectMapper;

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
        entityManager.flush();
    }

    private String validCourseJson() {
        return """
                {
                  "title": "Salsa Beginners",
                  "danceStyle": "SALSA",
                  "level": "BEGINNER",
                  "courseType": "PARTNER",
                  "description": "Learn the basics of Salsa",
                  "startDate": "%s",
                  "recurrenceType": "WEEKLY",
                  "numberOfSessions": 8,
                  "startTime": "19:00",
                  "endTime": "20:00",
                  "location": "Studio A",
                  "teachers": "Maria",
                  "maxParticipants": 15,
                  "roleBalancingEnabled": false,
                  "priceModel": "FIXED_COURSE",
                  "price": 180.00
                }
                """.formatted(LocalDate.now().plusDays(30));
    }

    @Nested
    class Create {

        @Test
        void createCourse_returnsCreatedId() throws Exception {
            mockMvc.perform(post("/api/courses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validCourseJson())
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").isNumber());
        }

        @Test
        void createCourse_persistsAllFields() throws Exception {
            mockMvc.perform(post("/api/courses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validCourseJson())
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isCreated());

            // Verify via GET /me
            mockMvc.perform(get("/api/courses/me")
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].title").value("Salsa Beginners"))
                    .andExpect(jsonPath("$[0].danceStyle").value("SALSA"))
                    .andExpect(jsonPath("$[0].level").value("BEGINNER"));
        }

        @Test
        void createCourse_returns400_whenTitleBlank() throws Exception {
            String json = validCourseJson().replace("\"Salsa Beginners\"", "\"\"");

            mockMvc.perform(post("/api/courses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.title").exists());
        }

        @Test
        void createCourse_returns400_whenRequiredFieldsMissing() throws Exception {
            String json = """
                    {
                      "title": "Test",
                      "location": "Studio A"
                    }
                    """;

            mockMvc.perform(post("/api/courses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors").exists());
        }

        @Test
        void createCourse_returns401_whenNotAuthenticated() throws Exception {
            mockMvc.perform(post("/api/courses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validCourseJson()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void createCourse_supportsNewDanceStyles() throws Exception {
            String json = validCourseJson().replace("\"SALSA\"", "\"KIZOMBA\"");

            mockMvc.perform(post("/api/courses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    class GetDetail {

        @Test
        void getDetail_returnsAllFields() throws Exception {
            Course course = createCourse(schoolA, "Bachata Advanced");
            entityManager.flush();

            mockMvc.perform(get("/api/courses/{id}", course.getId())
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(course.getId()))
                    .andExpect(jsonPath("$.title").value("Bachata Advanced"))
                    .andExpect(jsonPath("$.danceStyle").value("BACHATA"))
                    .andExpect(jsonPath("$.level").value("ADVANCED"))
                    .andExpect(jsonPath("$.courseType").value("PARTNER"))
                    .andExpect(jsonPath("$.startDate").exists())
                    .andExpect(jsonPath("$.recurrenceType").value("WEEKLY"))
                    .andExpect(jsonPath("$.dayOfWeek").exists())
                    .andExpect(jsonPath("$.numberOfSessions").value(10))
                    .andExpect(jsonPath("$.endDate").exists())
                    .andExpect(jsonPath("$.startTime").value("20:00:00"))
                    .andExpect(jsonPath("$.endTime").value("21:15:00"))
                    .andExpect(jsonPath("$.location").value("Studio A"))
                    .andExpect(jsonPath("$.maxParticipants").value(12))
                    .andExpect(jsonPath("$.roleBalancingEnabled").value(false))
                    .andExpect(jsonPath("$.priceModel").value("FIXED_COURSE"))
                    .andExpect(jsonPath("$.price").value(310.00))
                    .andExpect(jsonPath("$.status").value("OPEN"))
                    .andExpect(jsonPath("$.enrolledStudents").value(0))
                    .andExpect(jsonPath("$.completedSessions").value(0));
        }

        @Test
        void getDetail_returns404_forOtherSchoolsCourse() throws Exception {
            Course course = createCourse(schoolB, "School B Course");
            entityManager.flush();

            mockMvc.perform(get("/api/courses/{id}", course.getId())
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getDetail_returns404_forNonExistentId() throws Exception {
            mockMvc.perform(get("/api/courses/{id}", 99999)
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class Update {

        @Test
        void updateCourse_returnsUpdatedFields() throws Exception {
            Course course = createCourse(schoolA, "Original Title");
            entityManager.flush();

            String updateJson = validCourseJson().replace("\"Salsa Beginners\"", "\"Updated Title\"");

            mockMvc.perform(put("/api/courses/{id}", course.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(updateJson)
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Updated Title"))
                    .andExpect(jsonPath("$.danceStyle").value("SALSA"));
        }

        @Test
        void updateCourse_returns404_forOtherSchoolsCourse() throws Exception {
            Course course = createCourse(schoolB, "School B Course");
            entityManager.flush();

            mockMvc.perform(put("/api/courses/{id}", course.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validCourseJson())
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isNotFound());
        }

        @Test
        void updateCourse_returns400_whenValidationFails() throws Exception {
            Course course = createCourse(schoolA, "Test Course");
            entityManager.flush();

            String json = validCourseJson().replace("\"Salsa Beginners\"", "\"\"");

            mockMvc.perform(put("/api/courses/{id}", course.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class DomainRules {

        @Test
        void rejects_whenStartTimeNotBeforeEndTime() throws Exception {
            String json = validCourseJson()
                    .replace("\"19:00\"", "\"21:00\"")
                    .replace("\"20:00\"", "\"19:00\"");

            mockMvc.perform(post("/api/courses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("Start time must be before end time"));
        }

        @Test
        void rejects_whenStartDateInPast() throws Exception {
            String json = validCourseJson().replace(
                    LocalDate.now().plusDays(30).toString(),
                    LocalDate.now().minusDays(1).toString());

            mockMvc.perform(post("/api/courses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value("Start date must be in the future"));
        }

        @Test
        void rejects_whenThresholdSetWithoutBalancingEnabled() throws Exception {
            String json = validCourseJson()
                    .replace("\"priceModel\"", "\"roleBalanceThreshold\": 3, \"priceModel\"");

            mockMvc.perform(post("/api/courses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value(
                            "Role balance threshold requires role balancing to be enabled"));
        }

        @Test
        void accepts_partnerCourseWithRoleBalancingEnabled() throws Exception {
            String json = validCourseJson()
                    .replace("\"roleBalancingEnabled\": false",
                            "\"roleBalancingEnabled\": true, \"roleBalanceThreshold\": 3");

            mockMvc.perform(post("/api/courses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isCreated());
        }
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

    private Course createCourse(School school, String title) {
        LocalDate startDate = LocalDate.now().plusDays(30);
        Course course = new Course();
        course.setSchool(school);
        course.setTitle(title);
        course.setDanceStyle(DanceStyle.BACHATA);
        course.setLevel(CourseLevel.ADVANCED);
        course.setCourseType(CourseType.PARTNER);
        course.setStartDate(startDate);
        course.setRecurrenceType(RecurrenceType.WEEKLY);
        course.setDayOfWeek(startDate.getDayOfWeek());
        course.setNumberOfSessions(10);
        course.setEndDate(startDate.plusWeeks(9));
        course.setStartTime(LocalTime.of(20, 0));
        course.setEndTime(LocalTime.of(21, 15));
        course.setLocation("Studio A");
        course.setMaxParticipants(12);
        course.setPriceModel(PriceModel.FIXED_COURSE);
        course.setPrice(new BigDecimal("310.00"));
        course.setPublishedAt(LocalDate.now());
        entityManager.persist(course);
        return course;
    }

    private UsernamePasswordAuthenticationToken authToken(AppUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail());
        return new UsernamePasswordAuthenticationToken(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
    }
}
