package ch.ruppen.danceschool.course;

import ch.ruppen.danceschool.course.CourseEditPolicy.Tier;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CourseEditPolicyTest {

    @Nested
    class TierOf {
        @Test
        void draft_isFullyEditable_regardlessOfEnrollmentCount() {
            assertThat(CourseEditPolicy.tierOf(CourseLifecycleStatus.DRAFT, 0)).isEqualTo(Tier.FULLY_EDITABLE);
            assertThat(CourseEditPolicy.tierOf(CourseLifecycleStatus.DRAFT, 5)).isEqualTo(Tier.FULLY_EDITABLE);
        }

        @Test
        void open_withZeroEnrollments_isFullyEditable() {
            assertThat(CourseEditPolicy.tierOf(CourseLifecycleStatus.OPEN, 0)).isEqualTo(Tier.FULLY_EDITABLE);
        }

        @Test
        void open_withNonTerminalEnrollments_isRestricted() {
            assertThat(CourseEditPolicy.tierOf(CourseLifecycleStatus.OPEN, 1)).isEqualTo(Tier.RESTRICTED);
            assertThat(CourseEditPolicy.tierOf(CourseLifecycleStatus.OPEN, 50)).isEqualTo(Tier.RESTRICTED);
        }

        @Test
        void running_isReadOnly_regardlessOfEnrollmentCount() {
            assertThat(CourseEditPolicy.tierOf(CourseLifecycleStatus.RUNNING, 0)).isEqualTo(Tier.READ_ONLY);
            assertThat(CourseEditPolicy.tierOf(CourseLifecycleStatus.RUNNING, 10)).isEqualTo(Tier.READ_ONLY);
        }

        @Test
        void finished_isReadOnly_regardlessOfEnrollmentCount() {
            assertThat(CourseEditPolicy.tierOf(CourseLifecycleStatus.FINISHED, 0)).isEqualTo(Tier.READ_ONLY);
            assertThat(CourseEditPolicy.tierOf(CourseLifecycleStatus.FINISHED, 10)).isEqualTo(Tier.READ_ONLY);
        }
    }

    @Nested
    class IsFieldEditable {

        private static final List<String> LOCKED_FIELDS = List.of(
                "courseType", "price", "priceModel",
                "danceStyle", "level",
                "startDate", "endDate", "dayOfWeek",
                "startTime", "endTime",
                "numberOfSessions", "recurrenceType",
                "publishedAt");

        private static final List<String> COSMETIC_FIELDS = List.of(
                "title", "description", "teachers", "location",
                "maxParticipants", "roleBalanceThreshold");

        @ParameterizedTest
        @ValueSource(strings = {
                "title", "description", "teachers", "location",
                "maxParticipants", "roleBalanceThreshold",
                "courseType", "price", "priceModel", "danceStyle", "level",
                "startDate", "endDate", "dayOfWeek", "startTime", "endTime",
                "numberOfSessions", "recurrenceType", "publishedAt"})
        void fullyEditable_allowsEveryField(String field) {
            assertThat(CourseEditPolicy.isFieldEditable(Tier.FULLY_EDITABLE, field)).isTrue();
        }

        @Test
        void restricted_locksLockedFields() {
            for (String f : LOCKED_FIELDS) {
                assertThat(CourseEditPolicy.isFieldEditable(Tier.RESTRICTED, f))
                        .as("restricted must lock %s", f)
                        .isFalse();
            }
        }

        @Test
        void restricted_allowsCosmeticFields() {
            for (String f : COSMETIC_FIELDS) {
                assertThat(CourseEditPolicy.isFieldEditable(Tier.RESTRICTED, f))
                        .as("restricted must allow %s", f)
                        .isTrue();
            }
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "title", "description", "teachers", "location",
                "maxParticipants", "roleBalanceThreshold",
                "courseType", "price", "priceModel", "danceStyle", "level",
                "startDate", "endDate", "dayOfWeek", "startTime", "endTime",
                "numberOfSessions", "recurrenceType", "publishedAt"})
        void readOnly_locksEveryField(String field) {
            assertThat(CourseEditPolicy.isFieldEditable(Tier.READ_ONLY, field)).isFalse();
        }
    }

    @Nested
    class FindLockedFieldChanges {

        @Test
        void fullyEditable_neverReportsViolations() {
            Course current = sampleCourse();
            CreateCourseDto changed = dtoWith(current, d -> d.title("different").price(new BigDecimal("999.00")));
            assertThat(CourseEditPolicy.findLockedFieldChanges(current, changed, Tier.FULLY_EDITABLE)).isEmpty();
        }

        @Test
        void restricted_withIdenticalDto_reportsNone() {
            Course current = sampleCourse();
            CreateCourseDto same = dtoWith(current, d -> d);
            assertThat(CourseEditPolicy.findLockedFieldChanges(current, same, Tier.RESTRICTED)).isEmpty();
        }

        @Test
        void restricted_reportsChangedLockedFields_butNotCosmetic() {
            Course current = sampleCourse();
            CreateCourseDto changed = dtoWith(current, d -> d
                    .title("cosmetic change ok")
                    .location("also ok")
                    .danceStyle(DanceStyle.KIZOMBA)
                    .price(new BigDecimal("888.00")));
            assertThat(CourseEditPolicy.findLockedFieldChanges(current, changed, Tier.RESTRICTED))
                    .containsExactlyInAnyOrder("danceStyle", "price");
        }

        @Test
        void restricted_reportsEachLockedFieldIndividually() {
            Course current = sampleCourse();
            assertThat(CourseEditPolicy.findLockedFieldChanges(current,
                    dtoWith(current, d -> d.courseType(CourseType.SOLO)), Tier.RESTRICTED))
                    .containsExactly("courseType");
            assertThat(CourseEditPolicy.findLockedFieldChanges(current,
                    dtoWith(current, d -> d.level(CourseLevel.BEGINNER)), Tier.RESTRICTED))
                    .containsExactly("level");
            assertThat(CourseEditPolicy.findLockedFieldChanges(current,
                    dtoWith(current, d -> d.startDate(current.getStartDate().plusDays(1))), Tier.RESTRICTED))
                    .containsExactly("startDate");
            assertThat(CourseEditPolicy.findLockedFieldChanges(current,
                    dtoWith(current, d -> d.startTime(current.getStartTime().plusHours(1))), Tier.RESTRICTED))
                    .containsExactly("startTime");
            assertThat(CourseEditPolicy.findLockedFieldChanges(current,
                    dtoWith(current, d -> d.endTime(current.getEndTime().plusHours(1))), Tier.RESTRICTED))
                    .containsExactly("endTime");
            assertThat(CourseEditPolicy.findLockedFieldChanges(current,
                    dtoWith(current, d -> d.numberOfSessions(current.getNumberOfSessions() + 1)), Tier.RESTRICTED))
                    .containsExactly("numberOfSessions");
            assertThat(CourseEditPolicy.findLockedFieldChanges(current,
                    dtoWith(current, d -> d.price(current.getPrice().add(new BigDecimal("10")))), Tier.RESTRICTED))
                    .containsExactly("price");
            assertThat(CourseEditPolicy.findLockedFieldChanges(current,
                    dtoWith(current, d -> d.danceStyle(DanceStyle.BACHATA)), Tier.RESTRICTED))
                    .containsExactly("danceStyle");
        }

        @Test
        void readOnly_withIdenticalDto_reportsNone() {
            Course current = sampleCourse();
            CreateCourseDto same = dtoWith(current, d -> d);
            assertThat(CourseEditPolicy.findLockedFieldChanges(current, same, Tier.READ_ONLY)).isEmpty();
        }

        @Test
        void readOnly_reportsEvenCosmeticChanges() {
            Course current = sampleCourse();
            CreateCourseDto changed = dtoWith(current, d -> d.title("new title"));
            assertThat(CourseEditPolicy.findLockedFieldChanges(current, changed, Tier.READ_ONLY))
                    .containsExactly("title");
        }

        @Test
        void readOnly_reportsAllChangedFields() {
            Course current = sampleCourse();
            CreateCourseDto changed = dtoWith(current, d -> d
                    .title("x")
                    .location("y")
                    .maxParticipants(current.getMaxParticipants() + 1)
                    .price(new BigDecimal("777.00")));
            assertThat(CourseEditPolicy.findLockedFieldChanges(current, changed, Tier.READ_ONLY))
                    .containsExactlyInAnyOrder("title", "location", "maxParticipants", "price");
        }

        private Course sampleCourse() {
            Course c = new Course();
            c.setTitle("Salsa");
            c.setDanceStyle(DanceStyle.SALSA);
            c.setLevel(CourseLevel.INTERMEDIATE);
            c.setCourseType(CourseType.PARTNER);
            c.setDescription("desc");
            c.setStartDate(LocalDate.of(2026, 6, 1));
            c.setRecurrenceType(RecurrenceType.WEEKLY);
            c.setDayOfWeek(DayOfWeek.MONDAY);
            c.setNumberOfSessions(8);
            c.setEndDate(LocalDate.of(2026, 7, 20));
            c.setStartTime(LocalTime.of(19, 0));
            c.setEndTime(LocalTime.of(20, 0));
            c.setLocation("Studio A");
            c.setTeachers("Maria");
            c.setMaxParticipants(20);
            c.setRoleBalanceThreshold(3);
            c.setPriceModel(PriceModel.FIXED_COURSE);
            c.setPrice(new BigDecimal("180.00"));
            return c;
        }

        /**
         * Builder-like helper: start from a DTO matching the current course, apply the
         * caller's mutation, return the result.
         */
        private CreateCourseDto dtoWith(Course c, java.util.function.UnaryOperator<DtoBuilder> mutate) {
            DtoBuilder b = new DtoBuilder()
                    .title(c.getTitle())
                    .danceStyle(c.getDanceStyle())
                    .level(c.getLevel())
                    .courseType(c.getCourseType())
                    .description(c.getDescription())
                    .startDate(c.getStartDate())
                    .recurrenceType(c.getRecurrenceType())
                    .numberOfSessions(c.getNumberOfSessions())
                    .startTime(c.getStartTime())
                    .endTime(c.getEndTime())
                    .location(c.getLocation())
                    .teachers(c.getTeachers())
                    .maxParticipants(c.getMaxParticipants())
                    .roleBalanceThreshold(c.getRoleBalanceThreshold())
                    .priceModel(c.getPriceModel())
                    .price(c.getPrice());
            return mutate.apply(b).build();
        }

        private static final class DtoBuilder {
            private String title;
            private DanceStyle danceStyle;
            private CourseLevel level;
            private CourseType courseType;
            private String description;
            private LocalDate startDate;
            private RecurrenceType recurrenceType;
            private int numberOfSessions;
            private LocalTime startTime;
            private LocalTime endTime;
            private String location;
            private String teachers;
            private int maxParticipants;
            private Integer roleBalanceThreshold;
            private PriceModel priceModel;
            private BigDecimal price;

            DtoBuilder title(String v) { this.title = v; return this; }
            DtoBuilder danceStyle(DanceStyle v) { this.danceStyle = v; return this; }
            DtoBuilder level(CourseLevel v) { this.level = v; return this; }
            DtoBuilder courseType(CourseType v) { this.courseType = v; return this; }
            DtoBuilder description(String v) { this.description = v; return this; }
            DtoBuilder startDate(LocalDate v) { this.startDate = v; return this; }
            DtoBuilder recurrenceType(RecurrenceType v) { this.recurrenceType = v; return this; }
            DtoBuilder numberOfSessions(int v) { this.numberOfSessions = v; return this; }
            DtoBuilder startTime(LocalTime v) { this.startTime = v; return this; }
            DtoBuilder endTime(LocalTime v) { this.endTime = v; return this; }
            DtoBuilder location(String v) { this.location = v; return this; }
            DtoBuilder teachers(String v) { this.teachers = v; return this; }
            DtoBuilder maxParticipants(int v) { this.maxParticipants = v; return this; }
            DtoBuilder roleBalanceThreshold(Integer v) { this.roleBalanceThreshold = v; return this; }
            DtoBuilder priceModel(PriceModel v) { this.priceModel = v; return this; }
            DtoBuilder price(BigDecimal v) { this.price = v; return this; }

            CreateCourseDto build() {
                return new CreateCourseDto(title, danceStyle, level, courseType, description,
                        startDate, recurrenceType, numberOfSessions, startTime, endTime,
                        location, teachers, maxParticipants, roleBalanceThreshold, priceModel, price);
            }
        }
    }

}
