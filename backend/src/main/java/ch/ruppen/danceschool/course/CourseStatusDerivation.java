package ch.ruppen.danceschool.course;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Pure functions for deriving course lifecycle status and session progress from date fields.
 */
public final class CourseStatusDerivation {

    private CourseStatusDerivation() {
    }

    public static CourseLifecycleStatus deriveStatus(LocalDate publishedAt, LocalDate startDate,
                                                     LocalDate endDate, LocalDate today) {
        if (publishedAt == null) {
            return CourseLifecycleStatus.DRAFT;
        }
        if (today.isAfter(endDate)) {
            return CourseLifecycleStatus.FINISHED;
        }
        if (!today.isBefore(startDate)) {
            return CourseLifecycleStatus.RUNNING;
        }
        return CourseLifecycleStatus.OPEN;
    }

    public static int deriveCompletedSessions(LocalDate startDate, DayOfWeek dayOfWeek,
                                              int numberOfSessions, LocalDate today) {
        if (today.isBefore(startDate)) {
            return 0;
        }

        // Find the first session day (should be startDate itself if it matches dayOfWeek,
        // otherwise the next occurrence)
        LocalDate firstSession = startDate.getDayOfWeek() == dayOfWeek
                ? startDate
                : startDate.with(TemporalAdjusters.next(dayOfWeek));

        if (today.isBefore(firstSession)) {
            return 0;
        }

        // Number of weeks between first session and today, plus 1 for the first session itself
        long weeksBetween = (today.toEpochDay() - firstSession.toEpochDay()) / 7;
        int completed = (int) Math.min(weeksBetween + 1, numberOfSessions);
        return completed;
    }
}
