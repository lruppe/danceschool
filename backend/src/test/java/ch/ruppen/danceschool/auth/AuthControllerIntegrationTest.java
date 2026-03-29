package ch.ruppen.danceschool.auth;

import ch.ruppen.danceschool.TestSecurityConfig;
import ch.ruppen.danceschool.user.AppUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Test
    void me_returnsUser_whenValidJwt() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + TestSecurityConfig.VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TestSecurityConfig.TEST_EMAIL))
                .andExpect(jsonPath("$.name").value(TestSecurityConfig.TEST_NAME))
                .andExpect(jsonPath("$.memberships").isArray())
                .andExpect(jsonPath("$.memberships").isEmpty());
    }

    @Test
    void me_autoCreatesUser_onFirstRequest() throws Exception {
        // First request — user does not exist yet
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + TestSecurityConfig.VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(TestSecurityConfig.TEST_EMAIL));

        // Verify user was created in DB
        AppUser created = entityManager
                .createQuery("SELECT u FROM AppUser u WHERE u.firebaseUid = :uid", AppUser.class)
                .setParameter("uid", TestSecurityConfig.TEST_FIREBASE_UID)
                .getSingleResult();

        assert created != null;
        assert created.getEmail().equals(TestSecurityConfig.TEST_EMAIL);
    }

    @Test
    void me_reusesExistingUser_onSubsequentRequests() throws Exception {
        // First request creates the user
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + TestSecurityConfig.VALID_TOKEN))
                .andExpect(status().isOk());

        Long userId = entityManager
                .createQuery("SELECT u.id FROM AppUser u WHERE u.firebaseUid = :uid", Long.class)
                .setParameter("uid", TestSecurityConfig.TEST_FIREBASE_UID)
                .getSingleResult();

        // Second request reuses the same user
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + TestSecurityConfig.VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId));

        // Verify no duplicate
        long count = entityManager
                .createQuery("SELECT COUNT(u) FROM AppUser u WHERE u.firebaseUid = :uid", Long.class)
                .setParameter("uid", TestSecurityConfig.TEST_FIREBASE_UID)
                .getSingleResult();

        assert count == 1;
    }

    @Test
    void returns401_whenInvalidToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + TestSecurityConfig.INVALID_TOKEN))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returns401_whenMissingAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returns401_whenMalformedAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "NotBearer some-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginEndpoint_returns401_withoutToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"admin\",\"password\":\"pass\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginEndpoint_returnsNotFound_withValidToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header("Authorization", "Bearer " + TestSecurityConfig.VALID_TOKEN)
                        .contentType("application/json")
                        .content("{\"username\":\"admin\",\"password\":\"pass\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void logoutEndpoint_noLongerExists() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + TestSecurityConfig.VALID_TOKEN))
                .andExpect(status().isNotFound());
    }
}
