package ch.ruppen.danceschool.school;

import ch.ruppen.danceschool.TestSecurityConfig;
import ch.ruppen.danceschool.schoolmember.MemberRole;
import ch.ruppen.danceschool.schoolmember.SchoolMember;
import ch.ruppen.danceschool.shared.security.AuthenticatedUser;
import ch.ruppen.danceschool.shared.storage.ImageStorageService;
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

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
class SchoolImageDeletionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ImageStorageService imageStorageService;

    private AppUser testUser;
    private School school;

    @BeforeEach
    void setUp() {
        testUser = new AppUser();
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setFirebaseUid("test-firebase-uid");
        entityManager.persist(testUser);

        school = new School();
        school.setName("Test School");
        entityManager.persist(school);

        SchoolMember member = new SchoolMember();
        member.setUser(testUser);
        member.setSchool(school);
        member.setRole(MemberRole.OWNER);
        entityManager.persist(member);
        entityManager.flush();
    }

    @Test
    void updateMe_deletesOldCoverImage_whenReplaced() throws Exception {
        String oldCoverUrl = imageStorageService.store("old-cover".getBytes(), "cover.jpg");
        school.setCoverImageUrl(oldCoverUrl);
        entityManager.merge(school);
        entityManager.flush();

        Path oldFile = resolveStoredFile(oldCoverUrl);
        assertThat(oldFile).exists();

        String newCoverUrl = imageStorageService.store("new-cover".getBytes(), "cover2.jpg");

        mockMvc.perform(put("/api/schools/me")
                        .with(authentication(authToken(testUser)))
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Test School",
                                  "coverImageUrl": "%s"
                                }
                                """.formatted(newCoverUrl)))
                .andExpect(status().isOk());

        assertThat(oldFile).doesNotExist();
    }

    @Test
    void updateMe_deletesOldCoverImage_whenCleared() throws Exception {
        String oldCoverUrl = imageStorageService.store("cover-data".getBytes(), "cover.jpg");
        school.setCoverImageUrl(oldCoverUrl);
        entityManager.merge(school);
        entityManager.flush();

        Path oldFile = resolveStoredFile(oldCoverUrl);
        assertThat(oldFile).exists();

        mockMvc.perform(put("/api/schools/me")
                        .with(authentication(authToken(testUser)))
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Test School",
                                  "coverImageUrl": null
                                }
                                """))
                .andExpect(status().isOk());

        assertThat(oldFile).doesNotExist();
    }

    @Test
    void updateMe_deletesOldLogo_whenReplaced() throws Exception {
        String oldLogoUrl = imageStorageService.store("old-logo".getBytes(), "logo.png");
        school.setLogoUrl(oldLogoUrl);
        entityManager.merge(school);
        entityManager.flush();

        Path oldFile = resolveStoredFile(oldLogoUrl);
        assertThat(oldFile).exists();

        String newLogoUrl = imageStorageService.store("new-logo".getBytes(), "logo2.png");

        mockMvc.perform(put("/api/schools/me")
                        .with(authentication(authToken(testUser)))
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Test School",
                                  "logoUrl": "%s"
                                }
                                """.formatted(newLogoUrl)))
                .andExpect(status().isOk());

        assertThat(oldFile).doesNotExist();
    }

    @Test
    void updateMe_deletesRemovedGalleryImages() throws Exception {
        String galleryUrl1 = imageStorageService.store("gallery1".getBytes(), "g1.jpg");
        String galleryUrl2 = imageStorageService.store("gallery2".getBytes(), "g2.jpg");

        SchoolGalleryImage img1 = new SchoolGalleryImage();
        img1.setSchool(school);
        img1.setUrl(galleryUrl1);
        img1.setPosition(0);
        school.getGalleryImages().add(img1);

        SchoolGalleryImage img2 = new SchoolGalleryImage();
        img2.setSchool(school);
        img2.setUrl(galleryUrl2);
        img2.setPosition(1);
        school.getGalleryImages().add(img2);
        entityManager.merge(school);
        entityManager.flush();

        Path file1 = resolveStoredFile(galleryUrl1);
        Path file2 = resolveStoredFile(galleryUrl2);
        assertThat(file1).exists();
        assertThat(file2).exists();

        // Keep only the second image
        mockMvc.perform(put("/api/schools/me")
                        .with(authentication(authToken(testUser)))
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Test School",
                                  "galleryImages": [
                                    { "url": "%s", "position": 0 }
                                  ]
                                }
                                """.formatted(galleryUrl2)))
                .andExpect(status().isOk());

        assertThat(file1).doesNotExist();
        assertThat(file2).exists();
    }

    @Test
    void updateMe_succeeds_whenImageDeletionFails() throws Exception {
        // Set a cover URL that doesn't correspond to a real file —
        // deletion will fail but the update should still succeed
        school.setCoverImageUrl("http://localhost:8080/uploads/nonexistent-file.jpg");
        entityManager.merge(school);
        entityManager.flush();

        mockMvc.perform(put("/api/schools/me")
                        .with(authentication(authToken(testUser)))
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Test School",
                                  "coverImageUrl": "http://localhost:8080/uploads/new-cover.jpg"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void updateMe_doesNotDeleteImage_whenUrlUnchanged() throws Exception {
        String coverUrl = imageStorageService.store("cover-data".getBytes(), "cover.jpg");
        school.setCoverImageUrl(coverUrl);
        entityManager.merge(school);
        entityManager.flush();

        Path file = resolveStoredFile(coverUrl);
        assertThat(file).exists();

        mockMvc.perform(put("/api/schools/me")
                        .with(authentication(authToken(testUser)))
                        .contentType("application/json")
                        .content("""
                                {
                                  "name": "Updated Name",
                                  "coverImageUrl": "%s"
                                }
                                """.formatted(coverUrl)))
                .andExpect(status().isOk());

        assertThat(file).exists();
    }

    private Path resolveStoredFile(String url) {
        String key = ImageStorageService.extractKey(url);
        String storageDir = System.getProperty("java.io.tmpdir") + "/danceschool-uploads";
        return Path.of(storageDir, key);
    }

    private UsernamePasswordAuthenticationToken authToken(AppUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), null);
        return new UsernamePasswordAuthenticationToken(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
    }
}
