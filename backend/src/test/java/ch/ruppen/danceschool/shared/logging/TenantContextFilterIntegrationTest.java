package ch.ruppen.danceschool.shared.logging;

import ch.ruppen.danceschool.TestSecurityConfig;
import ch.ruppen.danceschool.school.School;
import ch.ruppen.danceschool.schoolmember.MemberRole;
import ch.ruppen.danceschool.schoolmember.SchoolMember;
import ch.ruppen.danceschool.shared.security.AuthenticatedUser;
import ch.ruppen.danceschool.user.AppUser;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that {@link TenantContextFilter} actually populates MDC during request handling —
 * catches regressions where the filter ends up auto-registered on the main servlet chain
 * (before Spring Security authenticates) and is then silently suppressed on the in-security
 * chain by the {@code ALREADY_FILTERED} guard.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestSecurityConfig.class)
class TenantContextFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        // ControllerLoggingAspect emits "Completed ..." on every request — captured events
        // include the MDC snapshot at log time.
        logAppender = new ListAppender<>();
        logAppender.start();
        controllerLogger().addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        controllerLogger().detachAppender(logAppender);
    }

    @Test
    void populatesUserIdAndSchoolIdInMdc_onAuthenticatedRequest() throws Exception {
        AppUser user = new AppUser();
        user.setEmail("mdc-test@example.com");
        user.setName("MDC Test");
        user.setFirebaseUid("mdc-test-uid");
        entityManager.persist(user);

        School school = new School();
        school.setName("MDC School");
        entityManager.persist(school);

        SchoolMember member = new SchoolMember();
        member.setUser(user);
        member.setSchool(school);
        member.setRole(MemberRole.OWNER);
        entityManager.persist(member);
        entityManager.flush();

        mockMvc.perform(get("/api/auth/me")
                        .with(authentication(authToken(user, school.getId()))))
                .andExpect(status().isOk());

        Map<String, String> mdc = lastEventMdc();
        assertThat(mdc).containsEntry("userId", String.valueOf(user.getId()));
        assertThat(mdc).containsEntry("schoolId", String.valueOf(school.getId()));
    }

    @Test
    void leavesSchoolIdEmpty_whenPrincipalHasNoSchool() throws Exception {
        AppUser user = new AppUser();
        user.setEmail("no-school@example.com");
        user.setName("No School");
        user.setFirebaseUid("no-school-uid");
        entityManager.persist(user);
        entityManager.flush();

        mockMvc.perform(get("/api/auth/me")
                        .with(authentication(authToken(user, null))))
                .andExpect(status().isOk());

        Map<String, String> mdc = lastEventMdc();
        assertThat(mdc).containsEntry("userId", String.valueOf(user.getId()));
        assertThat(mdc).doesNotContainKey("schoolId");
    }

    private Map<String, String> lastEventMdc() {
        List<ILoggingEvent> events = logAppender.list;
        assertThat(events).isNotEmpty();
        return events.getLast().getMDCPropertyMap();
    }

    private Logger controllerLogger() {
        return (Logger) LoggerFactory.getLogger(
                "ch.ruppen.danceschool.shared.logging.ControllerLoggingAspect");
    }

    private UsernamePasswordAuthenticationToken authToken(AppUser user, Long schoolId) {
        AuthenticatedUser principal = new AuthenticatedUser(user.getId(), user.getEmail(), schoolId);
        return new UsernamePasswordAuthenticationToken(
                principal, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
    }
}
