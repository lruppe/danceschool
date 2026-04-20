package ch.ruppen.danceschool.course;

import ch.ruppen.danceschool.TestSecurityConfig;
import ch.ruppen.danceschool.enrollment.DanceRole;
import ch.ruppen.danceschool.enrollment.Enrollment;
import ch.ruppen.danceschool.enrollment.EnrollmentStatus;
import ch.ruppen.danceschool.enrollment.WaitlistReason;
import ch.ruppen.danceschool.school.School;
import ch.ruppen.danceschool.schoolmember.MemberRole;
import ch.ruppen.danceschool.schoolmember.SchoolMember;
import ch.ruppen.danceschool.shared.security.AuthenticatedUser;
import ch.ruppen.danceschool.student.Student;
import ch.ruppen.danceschool.user.AppUser;
import jakarta.persistence.EntityManager;
import org.assertj.core.api.Assertions;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
                    .andExpect(jsonPath("$.roleBalanceThreshold").doesNotExist())
                    .andExpect(jsonPath("$.priceModel").value("FIXED_COURSE"))
                    .andExpect(jsonPath("$.price").value(310.00))
                    .andExpect(jsonPath("$.status").value("OPEN"))
                    .andExpect(jsonPath("$.enrolledStudents").value(0));
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
            // Draft course (publishedAt=null) → fully editable
            Course course = createDraftCourse(schoolA, "Original Title");
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
            Course course = createDraftCourse(schoolB, "School B Course");
            entityManager.flush();

            mockMvc.perform(put("/api/courses/{id}", course.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validCourseJson())
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isNotFound());
        }

        @Test
        void updateCourse_returns400_whenValidationFails() throws Exception {
            Course course = createDraftCourse(schoolA, "Test Course");
            entityManager.flush();

            String json = validCourseJson().replace("\"Salsa Beginners\"", "\"\"");

            mockMvc.perform(put("/api/courses/{id}", course.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void updateCourse_cosmeticChangesOnRestricted_returns200() throws Exception {
            Course course = createOpenCourse(schoolA, "Original Title");
            Student s = createStudent(schoolA, "Anna", "anna@example.com");
            persistEnrollment(course, s, DanceRole.LEAD, EnrollmentStatus.CONFIRMED, null, null);
            entityManager.flush();

            String json = jsonFor(course,
                    "\"Original Title\"", "\"New Title\"",
                    "\"Studio A\"", "\"Studio B\"",
                    "\"maxParticipants\": 12", "\"maxParticipants\": 14");

            mockMvc.perform(put("/api/courses/{id}", course.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("New Title"))
                    .andExpect(jsonPath("$.location").value("Studio B"))
                    .andExpect(jsonPath("$.maxParticipants").value(14))
                    .andExpect(jsonPath("$.editTier").value("RESTRICTED"));
        }

        @Test
        void updateCourse_lockedFieldChangeOnRestricted_returns409WithRejectedFields() throws Exception {
            Course course = createOpenCourse(schoolA, "Title");
            Student s = createStudent(schoolA, "Anna", "anna@example.com");
            persistEnrollment(course, s, DanceRole.LEAD, EnrollmentStatus.CONFIRMED, null, null);
            entityManager.flush();

            String json = jsonFor(course,
                    "\"BACHATA\"", "\"SALSA\"",        // flip danceStyle
                    "\"price\": 310.00", "\"price\": 999.00"); // flip price

            mockMvc.perform(put("/api/courses/{id}", course.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.tier").value("RESTRICTED"))
                    .andExpect(jsonPath("$.rejectedFields[*]").value(
                            org.hamcrest.Matchers.containsInAnyOrder("danceStyle", "price")));
        }

        @Test
        void updateCourse_anyChangeOnRunning_returns409() throws Exception {
            Course course = createRunningCourse(schoolA, "Running Course");
            entityManager.flush();

            // Flip only the title (a cosmetic field). READ_ONLY tier must still reject it.
            String json = jsonFor(course, "\"Running Course\"", "\"Renamed\"");

            mockMvc.perform(put("/api/courses/{id}", course.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.tier").value("READ_ONLY"))
                    .andExpect(jsonPath("$.rejectedFields[0]").value("title"));
        }

        @Test
        void updateCourse_anyChangeOnFinished_returns409() throws Exception {
            Course course = createFinishedCourse(schoolA, "Finished Course");
            entityManager.flush();

            String json = jsonFor(course, "\"Finished Course\"", "\"Renamed\"");

            mockMvc.perform(put("/api/courses/{id}", course.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.tier").value("READ_ONLY"));
        }

        @Test
        void updateCourse_openWithZeroEnrollments_allowsLockedFieldChange() throws Exception {
            Course course = createOpenCourse(schoolA, "Open No Enrollments");
            entityManager.flush();

            String json = jsonFor(course, "\"BACHATA\"", "\"SALSA\"");

            mockMvc.perform(put("/api/courses/{id}", course.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.danceStyle").value("SALSA"))
                    .andExpect(jsonPath("$.editTier").value("FULLY_EDITABLE"));
        }

        @Test
        void updateCourse_raisingMaxParticipants_promotesCapacityWaitlist() throws Exception {
            // max=2, 2 CONFIRMED seat-holders + 1 WAITLISTED(CAPACITY). Raise max to 3.
            Course course = createOpenCourseWithCapacity(schoolA, "Capacity Course", 2);
            Student a = createStudent(schoolA, "A", "a@example.com");
            Student b = createStudent(schoolA, "B", "b@example.com");
            Student c = createStudent(schoolA, "C", "c@example.com");
            persistEnrollment(course, a, DanceRole.LEAD, EnrollmentStatus.CONFIRMED, null, null);
            persistEnrollment(course, b, DanceRole.FOLLOW, EnrollmentStatus.CONFIRMED, null, null);
            Enrollment waitlisted = persistEnrollment(course, c, DanceRole.LEAD,
                    EnrollmentStatus.WAITLISTED, WaitlistReason.CAPACITY, 1);
            entityManager.flush();

            String json = jsonFor(course,
                    "\"maxParticipants\": 2", "\"maxParticipants\": 3");

            mockMvc.perform(put("/api/courses/{id}", course.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.maxParticipants").value(3));

            entityManager.flush();
            entityManager.refresh(waitlisted);
            Assertions.assertThat(waitlisted.getStatus()).isEqualTo(EnrollmentStatus.PENDING_PAYMENT);
            Assertions.assertThat(waitlisted.getWaitlistPosition()).isNull();
            Assertions.assertThat(waitlisted.getWaitlistReason()).isNull();
        }

        @Test
        void updateCourse_changingRoleBalanceThreshold_promotesRoleImbalanceWaitlist() throws Exception {
            // max=10, 2 LEADS CONFIRMED, 0 FOLLOWS; threshold=1 → 1 lead waitlisted by role imbalance.
            // Raise threshold to 5 → imbalance no longer blocks; lead should be promoted.
            Course course = createOpenCourseWithCapacityAndThreshold(schoolA, "Role Course", 10, 1);
            Student a = createStudent(schoolA, "A", "a@example.com");
            Student b = createStudent(schoolA, "B", "b@example.com");
            Student c = createStudent(schoolA, "C", "c@example.com");
            persistEnrollment(course, a, DanceRole.LEAD, EnrollmentStatus.CONFIRMED, null, null);
            persistEnrollment(course, b, DanceRole.LEAD, EnrollmentStatus.CONFIRMED, null, null);
            Enrollment waitlisted = persistEnrollment(course, c, DanceRole.LEAD,
                    EnrollmentStatus.WAITLISTED, WaitlistReason.ROLE_IMBALANCE, 1);
            entityManager.flush();

            String json = jsonFor(course,
                    "\"roleBalanceThreshold\": 1", "\"roleBalanceThreshold\": 5");

            mockMvc.perform(put("/api/courses/{id}", course.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isOk());

            entityManager.flush();
            entityManager.refresh(waitlisted);
            Assertions.assertThat(waitlisted.getStatus()).isEqualTo(EnrollmentStatus.PENDING_PAYMENT);
        }
    }

    @Nested
    class Delete {

        @Test
        void deleteCourse_returns204_whenDraft() throws Exception {
            Course course = createDraftCourse(schoolA, "Draft Course");
            Long id = course.getId();
            entityManager.flush();

            mockMvc.perform(delete("/api/courses/{id}", id)
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isNoContent());

            entityManager.flush();
            entityManager.clear();
            mockMvc.perform(get("/api/courses/{id}", id)
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isNotFound());
        }

        @Test
        void deleteCourse_returns409_whenOpen() throws Exception {
            // Published, startDate in future → OPEN
            Course course = createPublishedCourse(schoolA, "Open Course",
                    LocalDate.now().plusDays(30), LocalDate.now().minusDays(1));
            entityManager.flush();

            mockMvc.perform(delete("/api/courses/{id}", course.getId())
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail").value(
                            "Course " + course.getId() + " cannot be deleted: only unpublished (DRAFT) courses can be deleted."));
        }

        @Test
        void deleteCourse_returns409_whenRunning() throws Exception {
            // Published, startDate in past, endDate in future → RUNNING
            Course course = createPublishedCourse(schoolA, "Running Course",
                    LocalDate.now().minusDays(7), LocalDate.now().minusDays(14));
            entityManager.flush();

            mockMvc.perform(delete("/api/courses/{id}", course.getId())
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isConflict());
        }

        @Test
        void deleteCourse_returns409_whenFinished() throws Exception {
            // Published, endDate in past → FINISHED
            Course course = createPublishedCourse(schoolA, "Finished Course",
                    LocalDate.now().minusDays(90), LocalDate.now().minusDays(120));
            entityManager.flush();

            mockMvc.perform(delete("/api/courses/{id}", course.getId())
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isConflict());
        }

        @Test
        void deleteCourse_returns404_forOtherSchoolsCourse() throws Exception {
            Course course = createDraftCourse(schoolB, "School B Draft");
            entityManager.flush();

            mockMvc.perform(delete("/api/courses/{id}", course.getId())
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isNotFound());
        }

        @Test
        void deleteCourse_returns404_forNonExistentId() throws Exception {
            mockMvc.perform(delete("/api/courses/{id}", 99999)
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isNotFound());
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
        void rejects_whenRoleBalanceThresholdIsNegative() throws Exception {
            String json = validCourseJson()
                    .replace("\"priceModel\"", "\"roleBalanceThreshold\": -1, \"priceModel\"");

            mockMvc.perform(post("/api/courses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json)
                            .with(authentication(authToken(ownerA))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.roleBalanceThreshold").exists());
        }

        @Test
        void accepts_partnerCourseWithRoleBalanceThreshold() throws Exception {
            String json = validCourseJson()
                    .replace("\"priceModel\"", "\"roleBalanceThreshold\": 3, \"priceModel\"");

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
        // Default: OPEN course — published, startDate in the future.
        return persistCourse(school, title, LocalDate.now().plusDays(30),
                LocalDate.now(), 12, null);
    }

    private Course createDraftCourse(School school, String title) {
        return persistCourse(school, title, LocalDate.now().plusDays(30),
                null, 12, null);
    }

    private Course createOpenCourse(School school, String title) {
        return createCourse(school, title);
    }

    private Course createOpenCourseWithCapacity(School school, String title, int maxParticipants) {
        return persistCourse(school, title, LocalDate.now().plusDays(30),
                LocalDate.now(), maxParticipants, null);
    }

    private Course createOpenCourseWithCapacityAndThreshold(School school, String title,
                                                            int maxParticipants, Integer roleBalanceThreshold) {
        return persistCourse(school, title, LocalDate.now().plusDays(30),
                LocalDate.now(), maxParticipants, roleBalanceThreshold);
    }

    private Course createRunningCourse(School school, String title) {
        // startDate already passed, endDate still in the future.
        LocalDate startDate = LocalDate.now().minusDays(14);
        return persistCourse(school, title, startDate, startDate, 12, null);
    }

    private Course createFinishedCourse(School school, String title) {
        // Both startDate and endDate in the past.
        LocalDate startDate = LocalDate.now().minusWeeks(20);
        return persistCourse(school, title, startDate, startDate, 12, null);
    }

    private Course createPublishedCourse(School school, String title, LocalDate startDate, LocalDate publishedAt) {
        return persistCourse(school, title, startDate, publishedAt, 12, null);
    }

    private Course persistCourse(School school, String title, LocalDate startDate,
                                 LocalDate publishedAt, int maxParticipants,
                                 Integer roleBalanceThreshold) {
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
        course.setMaxParticipants(maxParticipants);
        course.setRoleBalanceThreshold(roleBalanceThreshold);
        course.setPriceModel(PriceModel.FIXED_COURSE);
        course.setPrice(new BigDecimal("310.00"));
        course.setPublishedAt(publishedAt);
        entityManager.persist(course);
        return course;
    }

    private Student createStudent(School school, String name, String email) {
        Student student = new Student();
        student.setSchool(school);
        student.setName(name);
        student.setEmail(email);
        entityManager.persist(student);
        return student;
    }

    private Enrollment persistEnrollment(Course course, Student student, DanceRole role,
                                         EnrollmentStatus status, WaitlistReason waitlistReason,
                                         Integer waitlistPosition) {
        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setStudent(student);
        enrollment.setDanceRole(role);
        enrollment.setStatus(status);
        enrollment.setEnrolledAt(Instant.now());
        enrollment.setWaitlistReason(waitlistReason);
        enrollment.setWaitlistPosition(waitlistPosition);
        entityManager.persist(enrollment);
        return enrollment;
    }

    /**
     * Builds a JSON payload that exactly matches the course's current field values, then
     * applies sequential find/replace pairs. Lets tests mutate a single field without the
     * rest of the DTO differing (which would look like a locked-field change to the policy).
     */
    private String jsonFor(Course course, String... replacements) {
        String teachersJson = course.getTeachers() == null ? "null" : "\"" + course.getTeachers() + "\"";
        String descriptionJson = course.getDescription() == null ? "null" : "\"" + course.getDescription() + "\"";
        String roleBalanceJson = course.getRoleBalanceThreshold() == null
                ? "null" : course.getRoleBalanceThreshold().toString();
        String json = """
                {
                  "title": "%s",
                  "danceStyle": "%s",
                  "level": "%s",
                  "courseType": "%s",
                  "description": %s,
                  "startDate": "%s",
                  "recurrenceType": "%s",
                  "numberOfSessions": %d,
                  "startTime": "%s",
                  "endTime": "%s",
                  "location": "%s",
                  "teachers": %s,
                  "maxParticipants": %d,
                  "roleBalanceThreshold": %s,
                  "priceModel": "%s",
                  "price": %s
                }
                """.formatted(
                        course.getTitle(),
                        course.getDanceStyle(),
                        course.getLevel(),
                        course.getCourseType(),
                        descriptionJson,
                        course.getStartDate(),
                        course.getRecurrenceType(),
                        course.getNumberOfSessions(),
                        course.getStartTime(),
                        course.getEndTime(),
                        course.getLocation(),
                        teachersJson,
                        course.getMaxParticipants(),
                        roleBalanceJson,
                        course.getPriceModel(),
                        course.getPrice());
        if ((replacements.length & 1) != 0) {
            throw new IllegalArgumentException("jsonFor replacements must be pairs");
        }
        for (int i = 0; i < replacements.length; i += 2) {
            if (!json.contains(replacements[i])) {
                throw new IllegalArgumentException(
                        "jsonFor: pattern not found in JSON: " + replacements[i]);
            }
            json = json.replace(replacements[i], replacements[i + 1]);
        }
        return json;
    }

    private UsernamePasswordAuthenticationToken authToken(AppUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), null);
        return new UsernamePasswordAuthenticationToken(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
    }
}
