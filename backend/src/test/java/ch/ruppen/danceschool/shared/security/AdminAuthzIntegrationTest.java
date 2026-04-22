package ch.ruppen.danceschool.shared.security;

import ch.ruppen.danceschool.TestSecurityConfig;
import ch.ruppen.danceschool.WithMockAppUser;
import ch.ruppen.danceschool.school.School;
import ch.ruppen.danceschool.schoolmember.MemberRole;
import ch.ruppen.danceschool.schoolmember.SchoolMember;
import ch.ruppen.danceschool.user.AppUser;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the admin-endpoint authorization gate end-to-end: the real
 * {@code securityFilterChain} is in play, {@code @PreAuthorize} runs,
 * {@code SchoolAuthz} queries {@code SchoolMemberRepository}. No mocks.
 *
 * <p>Covers the three acceptance-criteria buckets from #352:
 * <ul>
 *   <li>Unauthenticated → 401 (sampled per controller).</li>
 *   <li>Authenticated-no-membership → 403 on admin endpoints; 200 on self-info /
 *       onboarding.</li>
 *   <li>Authenticated-with-membership → 200 on own resources.</li>
 * </ul>
 *
 * <p>Cross-tenant 404 assertions live in the per-feature tenant-isolation tests
 * (CourseTenantIsolationTest, StudentTenantIsolationTest, EnrollmentIntegrationTest).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
class AdminAuthzIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    // ---- Unauthenticated → 401 (sampled per controller) ----

    @Test
    void unauthenticated_coursesMe_returns401() throws Exception {
        mockMvc.perform(get("/api/courses/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticated_schoolsMe_returns401() throws Exception {
        mockMvc.perform(get("/api/schools/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticated_students_returns401() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticated_paymentsMe_returns401() throws Exception {
        mockMvc.perform(get("/api/payments/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticated_enrollments_returns401() throws Exception {
        mockMvc.perform(put("/api/enrollments/{id}/mark-paid", 1L))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticated_imageUpload_returns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE,
                new byte[]{(byte) 0xFF, (byte) 0xD8});
        mockMvc.perform(multipart("/api/images").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticated_createSchool_returns401() throws Exception {
        mockMvc.perform(post("/api/schools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Some School" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticated_authMe_returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    // ---- Authenticated without membership → 403 on admin endpoints ----

    @Test
    @WithMockAppUser(userId = 9999L, email = "stranger@example.com")
    void noMembership_coursesMe_returns403() throws Exception {
        mockMvc.perform(get("/api/courses/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockAppUser(userId = 9999L, email = "stranger@example.com")
    void noMembership_schoolsMe_returns403() throws Exception {
        mockMvc.perform(get("/api/schools/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockAppUser(userId = 9999L, email = "stranger@example.com")
    void noMembership_updateSchoolMe_returns403() throws Exception {
        mockMvc.perform(put("/api/schools/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "Not Mine" }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockAppUser(userId = 9999L, email = "stranger@example.com")
    void noMembership_listStudents_returns403() throws Exception {
        mockMvc.perform(get("/api/students"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockAppUser(userId = 9999L, email = "stranger@example.com")
    void noMembership_getStudent_returns403() throws Exception {
        mockMvc.perform(get("/api/students/{id}", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockAppUser(userId = 9999L, email = "stranger@example.com")
    void noMembership_createStudent_returns403() throws Exception {
        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "X", "email": "x@example.com" }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockAppUser(userId = 9999L, email = "stranger@example.com")
    void noMembership_enrollmentMarkPaid_returns403() throws Exception {
        mockMvc.perform(put("/api/enrollments/{id}/mark-paid", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockAppUser(userId = 9999L, email = "stranger@example.com")
    void noMembership_enrollmentApprove_returns403() throws Exception {
        mockMvc.perform(put("/api/enrollments/{id}/approve", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockAppUser(userId = 9999L, email = "stranger@example.com")
    void noMembership_enrollmentReject_returns403() throws Exception {
        mockMvc.perform(put("/api/enrollments/{id}/reject", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockAppUser(userId = 9999L, email = "stranger@example.com")
    void noMembership_listCourseEnrollments_returns403() throws Exception {
        mockMvc.perform(get("/api/courses/{id}/enrollments", 1L))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockAppUser(userId = 9999L, email = "stranger@example.com")
    void noMembership_paymentsMe_returns403() throws Exception {
        mockMvc.perform(get("/api/payments/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockAppUser(userId = 9999L, email = "stranger@example.com")
    void noMembership_imageUpload_returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE,
                new byte[]{(byte) 0xFF, (byte) 0xD8});
        mockMvc.perform(multipart("/api/images").file(file))
                .andExpect(status().isForbidden());
    }

    // ---- Authenticated without membership → 200 on onboarding / self-info ----

    @Test
    void noMembership_createSchool_isAllowed() throws Exception {
        AppUser user = persistUser("onboarder@example.com", "Onboarder", "firebase-onboard");
        entityManager.flush();

        mockMvc.perform(post("/api/schools")
                        .with(authentication(tokenFor(user)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "name": "First School" }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void noMembership_authMe_isAllowed() throws Exception {
        AppUser user = persistUser("selfinfo@example.com", "Self Info", "firebase-self");
        entityManager.flush();

        mockMvc.perform(get("/api/auth/me")
                        .with(authentication(tokenFor(user))))
                .andExpect(status().isOk());
    }

    // ---- Authenticated with membership → 200 on own resources (smoke) ----

    @Test
    void withMembership_coursesMe_isAllowed() throws Exception {
        AppUser user = persistUser("member@example.com", "Member", "firebase-member");
        persistSchoolWithOwner("Member School", user);
        entityManager.flush();

        mockMvc.perform(get("/api/courses/me")
                        .with(authentication(tokenFor(user))))
                .andExpect(status().isOk());
    }

    @Test
    void withMembership_schoolsMe_isAllowed() throws Exception {
        AppUser user = persistUser("member2@example.com", "Member 2", "firebase-member2");
        persistSchoolWithOwner("Member School 2", user);
        entityManager.flush();

        mockMvc.perform(get("/api/schools/me")
                        .with(authentication(tokenFor(user))))
                .andExpect(status().isOk());
    }

    @Test
    void withMembership_listStudents_isAllowed() throws Exception {
        AppUser user = persistUser("member3@example.com", "Member 3", "firebase-member3");
        persistSchoolWithOwner("Member School 3", user);
        entityManager.flush();

        mockMvc.perform(get("/api/students")
                        .with(authentication(tokenFor(user))))
                .andExpect(status().isOk());
    }

    @Test
    void withMembership_paymentsMe_isAllowed() throws Exception {
        AppUser user = persistUser("member4@example.com", "Member 4", "firebase-member4");
        persistSchoolWithOwner("Member School 4", user);
        entityManager.flush();

        mockMvc.perform(get("/api/payments/me")
                        .with(authentication(tokenFor(user))))
                .andExpect(status().isOk());
    }

    // ---- Helpers ----

    private AppUser persistUser(String email, String name, String firebaseUid) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setName(name);
        user.setFirebaseUid(firebaseUid);
        entityManager.persist(user);
        return user;
    }

    private void persistSchoolWithOwner(String name, AppUser owner) {
        School school = new School();
        school.setName(name);
        entityManager.persist(school);
        SchoolMember member = new SchoolMember();
        member.setUser(owner);
        member.setSchool(school);
        member.setRole(MemberRole.OWNER);
        entityManager.persist(member);
    }

    private static UsernamePasswordAuthenticationToken tokenFor(AppUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), null);
        return new UsernamePasswordAuthenticationToken(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
    }
}
