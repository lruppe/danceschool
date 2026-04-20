package ch.ruppen.danceschool.school;

import ch.ruppen.danceschool.TestSecurityConfig;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
class SchoolTenantIsolationTest {

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

        entityManager.flush();
    }

    @Test
    void getMe_returnsOwnSchool_notOtherTenantSchool() throws Exception {
        mockMvc.perform(get("/api/schools/me")
                        .with(authentication(authToken(userA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("School A"))
                .andExpect(jsonPath("$.id").value(schoolA.getId()));

        mockMvc.perform(get("/api/schools/me")
                        .with(authentication(authToken(userB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("School B"))
                .andExpect(jsonPath("$.id").value(schoolB.getId()));
    }

    @Test
    void updateMe_updatesOwnSchool_notOtherTenantSchool() throws Exception {
        mockMvc.perform(put("/api/schools/me")
                        .with(authentication(authToken(userA)))
                        .contentType("application/json")
                        .content("""
                                { "name": "School A Updated" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("School A Updated"));

        // Verify school B is untouched
        mockMvc.perform(get("/api/schools/me")
                        .with(authentication(authToken(userB))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("School B"));
    }

    @Test
    void getMe_returns404_forUserWithNoSchool() throws Exception {
        AppUser orphan = createUser("orphan@example.com", "Orphan", "firebase-orphan");
        entityManager.flush();

        mockMvc.perform(get("/api/schools/me")
                        .with(authentication(authToken(orphan))))
                .andExpect(status().isNotFound());
    }

    @Test
    void multipleOwnersCanAccessSameSchool() throws Exception {
        AppUser coOwner = createUser("co-owner@example.com", "Co-Owner", "firebase-co");
        addOwner(schoolA, coOwner);
        entityManager.flush();

        // Both owners see the same school
        mockMvc.perform(get("/api/schools/me")
                        .with(authentication(authToken(userA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(schoolA.getId()));

        mockMvc.perform(get("/api/schools/me")
                        .with(authentication(authToken(coOwner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(schoolA.getId()));
    }

    @Test
    void coOwnerCanUpdateSharedSchool() throws Exception {
        AppUser coOwner = createUser("co-owner@example.com", "Co-Owner", "firebase-co");
        addOwner(schoolA, coOwner);
        entityManager.flush();

        mockMvc.perform(put("/api/schools/me")
                        .with(authentication(authToken(coOwner)))
                        .contentType("application/json")
                        .content("""
                                { "name": "Updated by Co-Owner" }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated by Co-Owner"));

        // Original owner sees the update
        mockMvc.perform(get("/api/schools/me")
                        .with(authentication(authToken(userA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated by Co-Owner"));
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

    private UsernamePasswordAuthenticationToken authToken(AppUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), null);
        return new UsernamePasswordAuthenticationToken(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
    }
}
