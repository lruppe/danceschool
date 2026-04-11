package ch.ruppen.danceschool.shared.logging;

import java.util.List;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BusinessLoggingAspectTest {

    private final BusinessLoggingAspect aspect = new BusinessLoggingAspect();
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        logAppender = new ListAppender<>();
        logAppender.start();
        Logger logger = (Logger) LoggerFactory.getLogger(BusinessLoggingAspect.class);
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(BusinessLoggingAspect.class);
        logger.detachAppender(logAppender);
    }

    @Test
    void logsEventWithLongAndStringArgs() {
        JoinPoint joinPoint = mockJoinPoint(
                new String[]{"userId", "email"},
                new Object[]{3L, "leon@test.com"});
        BusinessOperation op = mockOperation("UserOnboarded");

        aspect.logBusinessEvent(joinPoint, op, null);

        assertLogContains("BUSINESS | UserOnboarded | userId=3 email=\"leon@test.com\"");
    }

    @Test
    void logsEventWithRecordDtoArg() {
        JoinPoint joinPoint = mockJoinPoint(
                new String[]{"userId", "dto"},
                new Object[]{3L, new SampleDto("Bachata Beginners")});
        BusinessOperation op = mockOperation("CourseCreated");

        aspect.logBusinessEvent(joinPoint, op, 12L);

        assertLogContains("BUSINESS | CourseCreated | userId=3 title=\"Bachata Beginners\" resultId=12");
    }

    @Test
    void logsEventWithReturnValueFields() {
        JoinPoint joinPoint = mockJoinPoint(
                new String[]{"userId"},
                new Object[]{3L});
        BusinessOperation op = mockOperation("SchoolCreated");

        aspect.logBusinessEvent(joinPoint, op, new SampleResult(5L, "Tanzwerk"));

        assertLogContains("BUSINESS | SchoolCreated | userId=3 id=5 name=\"Tanzwerk\"");
    }

    @Test
    void logsEventWithNestedIds() {
        SampleParent school = new SampleParent(5L);
        SampleParent user = new SampleParent(7L);
        SampleMember member = new SampleMember(school, user, "OWNER");

        JoinPoint joinPoint = mockJoinPoint(
                new String[]{"member"},
                new Object[]{member});
        BusinessOperation op = mockOperation("MembershipCreated");

        SampleMember result = new SampleMember(school, user, "OWNER");
        result.setId(1L);

        aspect.logBusinessEvent(joinPoint, op, result);

        String logLine = lastLogMessage();
        assertThat(logLine).contains("BUSINESS | MembershipCreated");
        assertThat(logLine).contains("role=\"OWNER\"");
        assertThat(logLine).contains("schoolId=5");
        assertThat(logLine).contains("userId=7");
        assertThat(logLine).contains("id=1");
    }

    @Test
    void doesNotDuplicateKeysFromArgsAndResult() {
        JoinPoint joinPoint = mockJoinPoint(
                new String[]{"userId", "dto"},
                new Object[]{3L, new SampleDto("Salsa")});
        BusinessOperation op = mockOperation("CourseUpdated");

        aspect.logBusinessEvent(joinPoint, op, new SampleDetailResult(12L, "Salsa"));

        String logLine = lastLogMessage();
        // "title" should appear only once (from arg dto, not duplicated from result)
        assertThat(logLine.indexOf("title=")).isEqualTo(logLine.lastIndexOf("title="));
    }

    @Test
    void handlesNullArgs() {
        JoinPoint joinPoint = mockJoinPoint(
                new String[]{"userId", "name"},
                new Object[]{3L, null});
        BusinessOperation op = mockOperation("TestEvent");

        aspect.logBusinessEvent(joinPoint, op, null);

        assertLogContains("BUSINESS | TestEvent | userId=3");
    }

    // --- helpers ---

    private JoinPoint mockJoinPoint(String[] paramNames, Object[] args) {
        JoinPoint joinPoint = mock(JoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.getParameterNames()).thenReturn(paramNames);
        when(joinPoint.getArgs()).thenReturn(args);
        return joinPoint;
    }

    private BusinessOperation mockOperation(String event) {
        BusinessOperation op = mock(BusinessOperation.class);
        when(op.event()).thenReturn(event);
        return op;
    }

    private void assertLogContains(String expected) {
        assertThat(lastLogMessage()).isEqualTo(expected);
    }

    private String lastLogMessage() {
        List<ILoggingEvent> events = logAppender.list;
        assertThat(events).isNotEmpty();
        return events.getLast().getFormattedMessage();
    }

    // --- test doubles ---

    record SampleDto(String title) {}

    record SampleResult(Long id, String name) {}

    record SampleDetailResult(Long id, String title) {}

    static class SampleParent {
        private final Long id;
        SampleParent(Long id) { this.id = id; }
        public Long getId() { return id; }
    }

    static class SampleMember {
        private Long id;
        private final SampleParent school;
        private final SampleParent user;
        private final String role;

        SampleMember(SampleParent school, SampleParent user, String role) {
            this.school = school;
            this.user = user;
            this.role = role;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public SampleParent getSchool() { return school; }
        public SampleParent getUser() { return user; }
        public String getRole() { return role; }
    }
}
