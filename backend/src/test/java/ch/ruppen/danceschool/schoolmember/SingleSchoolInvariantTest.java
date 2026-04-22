package ch.ruppen.danceschool.schoolmember;

import ch.ruppen.danceschool.TestSecurityConfig;
import ch.ruppen.danceschool.school.School;
import ch.ruppen.danceschool.shared.security.AuthenticatedUser;
import ch.ruppen.danceschool.user.AppUser;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
class SingleSchoolInvariantTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SchoolMemberRepository schoolMemberRepository;

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
    void postSchoolsTwice_secondCallReturns409() throws Exception {
        mockMvc.perform(post("/api/schools")
                        .with(authentication(authToken(testUser)))
                        .contentType("application/json")
                        .content("""
                                { "name": "First School" }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/schools")
                        .with(authentication(authToken(testUser)))
                        .contentType("application/json")
                        .content("""
                                { "name": "Second School" }
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void savingDuplicateSchoolMember_violatesUniqueConstraint() {
        School school1 = new School();
        school1.setName("School 1");
        entityManager.persist(school1);

        School school2 = new School();
        school2.setName("School 2");
        entityManager.persist(school2);

        SchoolMember first = new SchoolMember();
        first.setUser(testUser);
        first.setSchool(school1);
        first.setRole(MemberRole.OWNER);
        schoolMemberRepository.saveAndFlush(first);

        SchoolMember duplicate = new SchoolMember();
        duplicate.setUser(testUser);
        duplicate.setSchool(school2);
        duplicate.setRole(MemberRole.OWNER);

        assertThatThrownBy(() -> schoolMemberRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private UsernamePasswordAuthenticationToken authToken(AppUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), null);
        return new UsernamePasswordAuthenticationToken(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
    }
}
