package ch.ruppen.danceschool.course;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CourseStatusDerivationTest {

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
