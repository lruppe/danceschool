package ch.ruppen.danceschool.course;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * Pure functions for deriving course lifecycle status and session progress from date fields.
 *
 * <h2>Derived Status Model</h2>
 * Course lifecycle status is never stored — it is computed from three fields:
 * <ul>
 *   <li>{@code publishedAt} (nullable) — when the owner published the course</li>
 *   <li>{@code startDate} — first session date</li>
 *   <li>{@code endDate} — last session date</li>
 * </ul>
 *
 * <h3>Derivation rules</h3>
 * <pre>
 * publishedAt == null                              → DRAFT
 * publishedAt != null  AND  today &lt; startDate      → OPEN
 * publishedAt != null  AND  startDate &lt;= today &lt;= endDate → RUNNING
 * today &gt; endDate                                  → FINISHED
 * </pre>
 *
 * <h3>Where the rules live (important!)</h3>
 * These same rules are duplicated in {@link CourseRepository} as JPQL queries for
 * database-level filtering ({@code findDraftBySchoolId}, {@code findOpenBySchoolId}, etc.).
 * If you change the derivation logic here, you <b>must</b> update the corresponding repository
 * queries and their integration tests in {@code CourseFilterIntegrationTest}.
 *
 * <h3>Design rationale</h3>
 * Time moves forward and the status follows — no manual transitions to forget, no inconsistent
 * states. The pattern extends naturally: {@code cancelledAt}, {@code archivedAt}, etc.
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
