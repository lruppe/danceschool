package ch.ruppen.danceschool.student;

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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
class StudentTenantIsolationTest {

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

    @Test
    void getStudent_returns404_forOtherSchoolsStudent() throws Exception {
        Student studentB = createStudent(schoolB, "Student B", "studentb@example.com");
        entityManager.flush();

        mockMvc.perform(get("/api/students/{id}", studentB.getId())
                        .with(authentication(authToken(ownerA))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getStudent_returnsOwnSchoolStudent() throws Exception {
        Student studentA = createStudent(schoolA, "Student A", "studenta@example.com");
        entityManager.flush();

        mockMvc.perform(get("/api/students/{id}", studentA.getId())
                        .with(authentication(authToken(ownerA))))
                .andExpect(status().isOk());
    }

    @Test
    void updateDanceLevels_returns404_forOtherSchoolsStudent() throws Exception {
        Student studentB = createStudent(schoolB, "Student B", "studentb@example.com");
        entityManager.flush();

        mockMvc.perform(put("/api/students/{id}/dance-levels", studentB.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "danceLevels": [
                                        {"danceStyle": "SALSA", "level": "BEGINNER"}
                                    ]
                                }
                                """)
                        .with(authentication(authToken(ownerA))))
                .andExpect(status().isNotFound());
    }

    @Test
    void createStudent_scopesToCallerSchool() throws Exception {
        // Owner A creates a student — it should be scoped to School A
        String response = mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Test Student",
                                    "email": "test@example.com"
                                }
                                """)
                        .with(authentication(authToken(ownerA))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long studentId = Long.parseLong(response.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1"));

        // Owner B cannot see Owner A's student
        mockMvc.perform(get("/api/students/{id}", studentId)
                        .with(authentication(authToken(ownerB))))
                .andExpect(status().isNotFound());
    }

    private AppUser createUser(String email, String name, String firebaseUid) {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setName(name);
        user.setFirebaseUid(firebaseUid);
        entityManager.persist(user);
        return user;
    }

    private School createSchoolWithOwner(String name, AppUser ownerUser) {
        School school = new School();
        school.setName(name);
        entityManager.persist(school);

        SchoolMember member = new SchoolMember();
        member.setUser(ownerUser);
        member.setSchool(school);
        member.setRole(MemberRole.OWNER);
        entityManager.persist(member);

        return school;
    }

    private Student createStudent(School school, String name, String email) {
        Student student = new Student();
        student.setSchool(school);
        student.setName(name);
        student.setEmail(email);
        entityManager.persist(student);
        return student;
    }

    private UsernamePasswordAuthenticationToken authToken(AppUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), null);
        return new UsernamePasswordAuthenticationToken(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
    }
}
