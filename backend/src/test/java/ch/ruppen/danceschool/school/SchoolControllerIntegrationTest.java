package ch.ruppen.danceschool.school;

import ch.ruppen.danceschool.TestSecurityConfig;
import ch.ruppen.danceschool.schoolmember.MemberRole;
import ch.ruppen.danceschool.schoolmember.SchoolMember;
import ch.ruppen.danceschool.shared.security.AuthenticatedUser;
import ch.ruppen.danceschool.user.AppUser;
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

import jakarta.persistence.EntityManager;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
class SchoolControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        testUser = new AppUser();
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setFirebaseUid("test-firebase-uid");
        entityManager.persist(testUser);
        entityManager.flush();
    }

    @Test
    void getMe_returnsSchoolDetail_whenUserIsOwner() throws Exception {
        School school = new School();
        school.setName("Test Dance School");
        school.setTagline("Dance with us");
        school.setAbout("A great school");
        school.setStreetAddress("123 Main St");
        school.setCity("Springfield");
        school.setPostalCode("62704");
        school.setCountry("USA");
        school.setPhone("+1 555-1234");
        school.setEmail("school@example.com");
        school.setWebsite("www.testschool.com");
        entityManager.persist(school);

        SchoolSpecialty specialty = new SchoolSpecialty();
        specialty.setSchool(school);
        specialty.setName("Bachata");
        school.getSpecialties().add(specialty);

        SchoolGalleryImage image = new SchoolGalleryImage();
        image.setSchool(school);
        image.setUrl("https://example.com/image1.jpg");
        image.setPosition(0);
        school.getGalleryImages().add(image);

        SchoolYoutubeVideo video = new SchoolYoutubeVideo();
        video.setSchool(school);
        video.setUrl("https://www.youtube.com/watch?v=abc123");
        video.setPosition(0);
        school.getYoutubeVideos().add(video);
        entityManager.merge(school);

        SchoolMember member = new SchoolMember();
        member.setUser(testUser);
        member.setSchool(school);
        member.setRole(MemberRole.OWNER);
        entityManager.persist(member);
        entityManager.flush();

        mockMvc.perform(get("/api/schools/me")
                        .with(authentication(authToken(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(school.getId()))
                .andExpect(jsonPath("$.name").value("Test Dance School"))
                .andExpect(jsonPath("$.tagline").value("Dance with us"))
                .andExpect(jsonPath("$.about").value("A great school"))
                .andExpect(jsonPath("$.streetAddress").value("123 Main St"))
                .andExpect(jsonPath("$.city").value("Springfield"))
                .andExpect(jsonPath("$.postalCode").value("62704"))
                .andExpect(jsonPath("$.country").value("USA"))
                .andExpect(jsonPath("$.phone").value("+1 555-1234"))
                .andExpect(jsonPath("$.email").value("school@example.com"))
                .andExpect(jsonPath("$.website").value("www.testschool.com"))
                .andExpect(jsonPath("$.coverImageUrl").isEmpty())
                .andExpect(jsonPath("$.logoUrl").isEmpty())
                .andExpect(jsonPath("$.specialties[0]").value("Bachata"))
                .andExpect(jsonPath("$.galleryImages[0].url").value("https://example.com/image1.jpg"))
                .andExpect(jsonPath("$.galleryImages[0].position").value(0))
                .andExpect(jsonPath("$.youtubeVideos[0].url").value("https://www.youtube.com/watch?v=abc123"))
                .andExpect(jsonPath("$.youtubeVideos[0].position").value(0));
    }

    @Test
    void getMe_returnsEmptyCollections_whenSchoolHasNoRelatedData() throws Exception {
        School school = new School();
        school.setName("Empty School");
        entityManager.persist(school);

        SchoolMember member = new SchoolMember();
        member.setUser(testUser);
        member.setSchool(school);
        member.setRole(MemberRole.OWNER);
        entityManager.persist(member);
        entityManager.flush();

        mockMvc.perform(get("/api/schools/me")
                        .with(authentication(authToken(testUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Empty School"))
                .andExpect(jsonPath("$.tagline").doesNotExist())
                .andExpect(jsonPath("$.specialties").isArray())
                .andExpect(jsonPath("$.specialties").isEmpty())
                .andExpect(jsonPath("$.galleryImages").isArray())
                .andExpect(jsonPath("$.galleryImages").isEmpty())
                .andExpect(jsonPath("$.youtubeVideos").isArray())
                .andExpect(jsonPath("$.youtubeVideos").isEmpty());
    }

    @Test
    void getMe_returns404_whenUserHasNoSchool() throws Exception {
        mockMvc.perform(get("/api/schools/me")
                        .with(authentication(authToken(testUser))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMe_returnsUnauthorized_whenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/schools/me"))
                .andExpect(status().isUnauthorized());
    }

    // --- PUT /api/schools/me tests ---

    @Test
    void updateMe_updatesAllFields_whenValid() throws Exception {
        School school = new School();
        school.setName("Old Name");
        entityManager.persist(school);

        SchoolMember member = new SchoolMember();
        member.setUser(testUser);
        member.setSchool(school);
        member.setRole(MemberRole.OWNER);
        entityManager.persist(member);
        entityManager.flush();

        mockMvc.perform(put("/api/schools/me")
                        .with(authentication(authToken(testUser)))
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Updated School",
                                  "tagline": "New Tagline",
                                  "about": "New about text",
                                  "streetAddress": "456 Dance Ave",
                                  "city": "Miami",
                                  "postalCode": "33101",
                                  "country": "US",
                                  "phone": "+1 305-555-0100",
                                  "email": "info@updated.com",
                                  "website": "https://www.updated.com",
                                  "coverImageUrl": "https://r2.example.com/cover.jpg",
                                  "logoUrl": "https://r2.example.com/logo.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated School"))
                .andExpect(jsonPath("$.tagline").value("New Tagline"))
                .andExpect(jsonPath("$.about").value("New about text"))
                .andExpect(jsonPath("$.streetAddress").value("456 Dance Ave"))
                .andExpect(jsonPath("$.city").value("Miami"))
                .andExpect(jsonPath("$.postalCode").value("33101"))
                .andExpect(jsonPath("$.country").value("US"))
                .andExpect(jsonPath("$.phone").value("+1 305-555-0100"))
                .andExpect(jsonPath("$.email").value("info@updated.com"))
                .andExpect(jsonPath("$.website").value("https://www.updated.com"))
                .andExpect(jsonPath("$.coverImageUrl").value("https://r2.example.com/cover.jpg"))
                .andExpect(jsonPath("$.logoUrl").value("https://r2.example.com/logo.png"));
    }

    @Test
    void updateMe_returns400_whenNameIsBlank() throws Exception {
        School school = new School();
        school.setName("Test School");
        entityManager.persist(school);

        SchoolMember member = new SchoolMember();
        member.setUser(testUser);
        member.setSchool(school);
        member.setRole(MemberRole.OWNER);
        entityManager.persist(member);
        entityManager.flush();

        mockMvc.perform(put("/api/schools/me")
                        .with(authentication(authToken(testUser)))
                        .contentType("application/json")
                        .content("""
                                { "name": "" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void updateMe_returns400_whenEmailInvalid() throws Exception {
        School school = new School();
        school.setName("Test School");
        entityManager.persist(school);

        SchoolMember member = new SchoolMember();
        member.setUser(testUser);
        member.setSchool(school);
        member.setRole(MemberRole.OWNER);
        entityManager.persist(member);
        entityManager.flush();

        mockMvc.perform(put("/api/schools/me")
                        .with(authentication(authToken(testUser)))
                        .contentType("application/json")
                        .content("""
                                { "name": "Valid Name", "email": "not-an-email" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void updateMe_returns404_whenUserHasNoSchool() throws Exception {
        mockMvc.perform(put("/api/schools/me")
                        .with(authentication(authToken(testUser)))
                        .contentType("application/json")
                        .content("""
                                { "name": "Some School" }
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateMe_returnsUnauthorized_whenNotAuthenticated() throws Exception {
        mockMvc.perform(put("/api/schools/me")
                        .contentType("application/json")
                        .content("""
                                { "name": "Some School" }
                                """))
                .andExpect(status().isUnauthorized());
    }

    private UsernamePasswordAuthenticationToken authToken(AppUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail());
        return new UsernamePasswordAuthenticationToken(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
    }
}
