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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
class StudentCrudIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    private AppUser owner;

    @BeforeEach
    void setUp() {
        owner = createUser("owner@example.com", "Owner", "firebase-owner");
        createSchoolWithOwner("Test School", owner);
        entityManager.flush();
    }

    @Test
    void createStudent_returnsId() throws Exception {
        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Anna Müller",
                                    "email": "anna@example.com",
                                    "phoneNumber": "+41 79 100 0001",
                                    "danceLevels": [
                                        {"danceStyle": "SALSA", "level": "INTERMEDIATE"},
                                        {"danceStyle": "BACHATA", "level": "BEGINNER"}
                                    ]
                                }
                                """)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void createStudent_withoutDanceLevels_returnsId() throws Exception {
        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "David Kim",
                                    "email": "david@example.com"
                                }
                                """)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());
    }

    @Test
    void createStudent_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "name": "Bad Email",
                                    "email": "not-an-email"
                                }
                                """)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createStudent_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "email": "test@example.com"
                                }
                                """)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getStudent_returnsProfileWithDanceLevels() throws Exception {
        Student student = createStudent("Anna Müller", "anna@example.com", "+41 79 100 0001");
        addDanceLevel(student, "SALSA", "INTERMEDIATE");
        addDanceLevel(student, "BACHATA", "BEGINNER");
        entityManager.flush();

        mockMvc.perform(get("/api/students/{id}", student.getId())
                        .with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(student.getId()))
                .andExpect(jsonPath("$.name").value("Anna Müller"))
                .andExpect(jsonPath("$.email").value("anna@example.com"))
                .andExpect(jsonPath("$.phoneNumber").value("+41 79 100 0001"))
                .andExpect(jsonPath("$.danceLevels.length()").value(2));
    }

    @Test
    void getStudent_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/students/{id}", 9999)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateDanceLevels_replacesAllLevels() throws Exception {
        Student student = createStudent("Anna Müller", "anna@example.com", null);
        addDanceLevel(student, "SALSA", "BEGINNER");
        entityManager.flush();

        mockMvc.perform(put("/api/students/{id}/dance-levels", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "danceLevels": [
                                        {"danceStyle": "SALSA", "level": "ADVANCED"},
                                        {"danceStyle": "KIZOMBA", "level": "STARTER"}
                                    ]
                                }
                                """)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.danceLevels.length()").value(2))
                .andExpect(jsonPath("$.danceLevels[?(@.danceStyle=='SALSA')].level").value("ADVANCED"))
                .andExpect(jsonPath("$.danceLevels[?(@.danceStyle=='KIZOMBA')].level").value("STARTER"));
    }

    @Test
    void updateDanceLevels_clearAll() throws Exception {
        Student student = createStudent("Anna Müller", "anna@example.com", null);
        addDanceLevel(student, "SALSA", "BEGINNER");
        entityManager.flush();

        mockMvc.perform(put("/api/students/{id}/dance-levels", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "danceLevels": []
                                }
                                """)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.danceLevels.length()").value(0));
    }

    @Test
    void updateDanceLevels_keepsUnchangedStylesAndAppendsNew() throws Exception {
        Student student = createStudent("Anna Müller", "anna@example.com", null);
        addDanceLevel(student, "SALSA", "INTERMEDIATE");
        addDanceLevel(student, "BACHATA", "BEGINNER");
        entityManager.flush();

        mockMvc.perform(put("/api/students/{id}/dance-levels", student.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "danceLevels": [
                                        {"danceStyle": "SALSA", "level": "INTERMEDIATE"},
                                        {"danceStyle": "BACHATA", "level": "BEGINNER"},
                                        {"danceStyle": "KIZOMBA", "level": "STARTER"}
                                    ]
                                }
                                """)
                        .with(authentication(authToken(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.danceLevels.length()").value(3))
                .andExpect(jsonPath("$.danceLevels[?(@.danceStyle=='SALSA')].level").value("INTERMEDIATE"))
                .andExpect(jsonPath("$.danceLevels[?(@.danceStyle=='BACHATA')].level").value("BEGINNER"))
                .andExpect(jsonPath("$.danceLevels[?(@.danceStyle=='KIZOMBA')].level").value("STARTER"));
    }

    @Test
    void updateDanceLevels_notFound_returns404() throws Exception {
        mockMvc.perform(put("/api/students/{id}/dance-levels", 9999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "danceLevels": []
                                }
                                """)
                        .with(authentication(authToken(owner))))
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

    private Student createStudent(String name, String email, String phoneNumber) {
        School school = entityManager.createQuery(
                        "SELECT s FROM School s JOIN SchoolMember m ON m.school = s WHERE m.user = :user", School.class)
                .setParameter("user", owner)
                .getSingleResult();

        Student student = new Student();
        student.setSchool(school);
        student.setName(name);
        student.setEmail(email);
        student.setPhoneNumber(phoneNumber);
        entityManager.persist(student);
        return student;
    }

    private void addDanceLevel(Student student, String style, String level) {
        StudentDanceLevel dl = new StudentDanceLevel();
        dl.setStudent(student);
        dl.setDanceStyle(ch.ruppen.danceschool.course.DanceStyle.valueOf(style));
        dl.setLevel(ch.ruppen.danceschool.course.CourseLevel.valueOf(level));
        student.getDanceLevels().add(dl);
        entityManager.persist(dl);
    }

    private UsernamePasswordAuthenticationToken authToken(AppUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), null);
        return new UsernamePasswordAuthenticationToken(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
    }
}
