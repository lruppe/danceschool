package ch.ruppen.danceschool.image;

import ch.ruppen.danceschool.TestSecurityConfig;
import ch.ruppen.danceschool.shared.security.AuthenticatedUser;
import ch.ruppen.danceschool.user.AppUser;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
class ImageControllerIntegrationTest {

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
    void upload_returnsUrl_whenValidJpeg() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE,
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});

        mockMvc.perform(multipart("/api/images")
                        .file(file)
                        .with(authentication(authToken(testUser))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").isString())
                .andExpect(jsonPath("$.url").isNotEmpty());
    }

    @Test
    void upload_returnsUrl_whenValidPng() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.png", MediaType.IMAGE_PNG_VALUE,
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});

        mockMvc.perform(multipart("/api/images")
                        .file(file)
                        .with(authentication(authToken(testUser))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").isString());
    }

    @Test
    void upload_returnsUrl_whenValidWebp() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.webp", "image/webp",
                new byte[]{0x52, 0x49, 0x46, 0x46});

        mockMvc.perform(multipart("/api/images")
                        .file(file)
                        .with(authentication(authToken(testUser))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").isString());
    }

    @Test
    void upload_returns400_whenFileExceeds5MB() throws Exception {
        byte[] oversized = new byte[6 * 1024 * 1024]; // 6 MB
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.jpg", MediaType.IMAGE_JPEG_VALUE, oversized);

        mockMvc.perform(multipart("/api/images")
                        .file(file)
                        .with(authentication(authToken(testUser))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upload_returns400_whenContentTypeIsGif() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "anim.gif", MediaType.IMAGE_GIF_VALUE,
                new byte[]{0x47, 0x49, 0x46, 0x38});

        mockMvc.perform(multipart("/api/images")
                        .file(file)
                        .with(authentication(authToken(testUser))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upload_returns400_whenContentTypeIsTextPlain() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "hack.txt", MediaType.TEXT_PLAIN_VALUE,
                "not an image".getBytes());

        mockMvc.perform(multipart("/api/images")
                        .file(file)
                        .with(authentication(authToken(testUser))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void upload_returnsUnauthorized_whenNotAuthenticated() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE,
                new byte[]{(byte) 0xFF, (byte) 0xD8});

        mockMvc.perform(multipart("/api/images").file(file))
                .andExpect(status().isUnauthorized());
    }

    private UsernamePasswordAuthenticationToken authToken(AppUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), null);
        return new UsernamePasswordAuthenticationToken(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
    }
}
