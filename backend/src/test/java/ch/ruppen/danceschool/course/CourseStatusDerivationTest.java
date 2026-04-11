package ch.ruppen.danceschool.course;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CourseStatusDerivationTest {

    @Nested
    class DeriveStatus {

        private final LocalDate startDate = LocalDate.of(2026, 5, 4);   // Monday
        private final LocalDate endDate = LocalDate.of(2026, 6, 22);    // Monday, 8 weeks later

        @Test
        void draft_whenPublishedAtIsNull() {
            assertThat(CourseStatusDerivation.deriveStatus(null, startDate, endDate, LocalDate.of(2026, 4, 1)))
                    .isEqualTo(CourseLifecycleStatus.DRAFT);
        }

        @Test
        void draft_whenUnpublishedEvenPastEndDate() {
            assertThat(CourseStatusDerivation.deriveStatus(null, startDate, endDate, LocalDate.of(2026, 7, 1)))
                    .isEqualTo(CourseLifecycleStatus.DRAFT);
        }

        @Test
        void open_whenPublishedAndTodayBeforeStartDate() {
            LocalDate publishedAt = LocalDate.of(2026, 4, 1);
            assertThat(CourseStatusDerivation.deriveStatus(publishedAt, startDate, endDate, LocalDate.of(2026, 4, 15)))
                    .isEqualTo(CourseLifecycleStatus.OPEN);
        }

        @Test
        void running_whenTodayEqualsStartDate() {
            LocalDate publishedAt = LocalDate.of(2026, 4, 1);
            assertThat(CourseStatusDerivation.deriveStatus(publishedAt, startDate, endDate, startDate))
                    .isEqualTo(CourseLifecycleStatus.RUNNING);
        }

        @Test
        void running_whenTodayBetweenStartAndEnd() {
            LocalDate publishedAt = LocalDate.of(2026, 4, 1);
            assertThat(CourseStatusDerivation.deriveStatus(publishedAt, startDate, endDate, LocalDate.of(2026, 5, 20)))
                    .isEqualTo(CourseLifecycleStatus.RUNNING);
        }

        @Test
        void running_whenTodayEqualsEndDate() {
            LocalDate publishedAt = LocalDate.of(2026, 4, 1);
            assertThat(CourseStatusDerivation.deriveStatus(publishedAt, startDate, endDate, endDate))
                    .isEqualTo(CourseLifecycleStatus.RUNNING);
        }

        @Test
        void finished_whenTodayIsOneDayAfterEndDate() {
            LocalDate publishedAt = LocalDate.of(2026, 4, 1);
            assertThat(CourseStatusDerivation.deriveStatus(publishedAt, startDate, endDate, endDate.plusDays(1)))
                    .isEqualTo(CourseLifecycleStatus.FINISHED);
        }

        @Test
        void finished_whenTodayWellPastEndDate() {
            LocalDate publishedAt = LocalDate.of(2026, 4, 1);
            assertThat(CourseStatusDerivation.deriveStatus(publishedAt, startDate, endDate, LocalDate.of(2026, 12, 1)))
                    .isEqualTo(CourseLifecycleStatus.FINISHED);
        }
    }

    @Nested
    class DeriveCompletedSessions {

        private final LocalDate startDate = LocalDate.of(2026, 5, 4);  // Monday
        private final DayOfWeek dayOfWeek = DayOfWeek.MONDAY;
        private final int numberOfSessions = 8;

        @Test
        void zero_whenTodayBeforeStartDate() {
            assertThat(CourseStatusDerivation.deriveCompletedSessions(startDate, dayOfWeek, numberOfSessions,
                    LocalDate.of(2026, 4, 15)))
                    .isEqualTo(0);
        }

        @Test
        void one_whenTodayIsFirstSessionDay() {
            assertThat(CourseStatusDerivation.deriveCompletedSessions(startDate, dayOfWeek, numberOfSessions,
                    startDate))
                    .isEqualTo(1);
        }

        @Test
        void one_whenTodayBetweenFirstAndSecondSession() {
            assertThat(CourseStatusDerivation.deriveCompletedSessions(startDate, dayOfWeek, numberOfSessions,
                    startDate.plusDays(3)))  // Thursday of first week
                    .isEqualTo(1);
        }

        @Test
        void two_whenTodayIsSecondSessionDay() {
            assertThat(CourseStatusDerivation.deriveCompletedSessions(startDate, dayOfWeek, numberOfSessions,
                    startDate.plusWeeks(1)))
                    .isEqualTo(2);
        }

        @Test
        void allSessions_whenTodayAfterLastSession() {
            assertThat(CourseStatusDerivation.deriveCompletedSessions(startDate, dayOfWeek, numberOfSessions,
                    startDate.plusWeeks(10)))
                    .isEqualTo(8);
        }

        @Test
        void cappedAtNumberOfSessions() {
            assertThat(CourseStatusDerivation.deriveCompletedSessions(startDate, dayOfWeek, numberOfSessions,
                    startDate.plusWeeks(100)))
                    .isEqualTo(8);
        }

        @Test
        void correctCount_midCourse() {
            // After 4 weeks: sessions on week 0, 1, 2, 3, 4 = 5 sessions
            assertThat(CourseStatusDerivation.deriveCompletedSessions(startDate, dayOfWeek, numberOfSessions,
                    startDate.plusWeeks(4)))
                    .isEqualTo(5);
        }
    }
}
