package ch.ruppen.danceschool.course;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Pure functions that answer "may this course be edited, and which fields?". Reuses
 * {@link CourseStatusDerivation#deriveStatus} as the single source of truth for lifecycle
 * status — no parallel state machine.
 *
 * <h2>Tier model</h2>
 * <pre>
 * DRAFT                              → FULLY_EDITABLE
 * OPEN,     0 non-terminal enrolments → FULLY_EDITABLE
 * OPEN,   ≥ 1 non-terminal enrolments → RESTRICTED (cosmetic fields still editable)
 * RUNNING  (any count)                → READ_ONLY
 * FINISHED (any count)                → READ_ONLY
 * </pre>
 *
 * "Cosmetic" = {@code title, description, teachers, location, maxParticipants,
 * roleBalanceThreshold}. Everything else is locked in the RESTRICTED tier, and every field is
 * locked in READ_ONLY.
 */
public final class CourseEditPolicy {

    public enum Tier {
        FULLY_EDITABLE,
        RESTRICTED,
        READ_ONLY
    }

    static final Set<String> LOCKED_IN_RESTRICTED = Set.of(
            "courseType", "price", "priceModel",
            "danceStyle", "level",
            "startDate", "endDate", "dayOfWeek",
            "startTime", "endTime",
            "numberOfSessions", "recurrenceType",
            "publishedAt"
    );

    private CourseEditPolicy() {
    }

    public static Tier tierOf(CourseLifecycleStatus status, int nonTerminalEnrollments) {
        return switch (status) {
            case DRAFT -> Tier.FULLY_EDITABLE;
            case OPEN -> nonTerminalEnrollments >= 1 ? Tier.RESTRICTED : Tier.FULLY_EDITABLE;
            case RUNNING, FINISHED -> Tier.READ_ONLY;
        };
    }

    public static boolean isFieldEditable(Tier tier, String field) {
        return switch (tier) {
            case FULLY_EDITABLE -> true;
            case RESTRICTED -> !LOCKED_IN_RESTRICTED.contains(field);
            case READ_ONLY -> false;
        };
    }

    /**
     * Compares the incoming DTO against the stored entity and returns the DTO-present field
     * names that the given tier doesn't allow to change. {@code endDate}, {@code dayOfWeek}
     * and {@code publishedAt} are intentionally skipped — they are derived or server-owned
     * and not part of the DTO contract.
     */
    public static List<String> findLockedFieldChanges(Course current, CreateCourseDto incoming, Tier tier) {
        if (tier == Tier.FULLY_EDITABLE) {
            return List.of();
        }
        List<String> rejected = new ArrayList<>();

        // Fields locked in both RESTRICTED and READ_ONLY
        if (!Objects.equals(current.getCourseType(), incoming.courseType())) rejected.add("courseType");
        if (!Objects.equals(current.getDanceStyle(), incoming.danceStyle())) rejected.add("danceStyle");
        if (!Objects.equals(current.getLevel(), incoming.level())) rejected.add("level");
        if (!Objects.equals(current.getPriceModel(), incoming.priceModel())) rejected.add("priceModel");
        if (priceChanged(current.getPrice(), incoming.price())) rejected.add("price");
        if (!Objects.equals(current.getStartDate(), incoming.startDate())) rejected.add("startDate");
        if (!Objects.equals(current.getStartTime(), incoming.startTime())) rejected.add("startTime");
        if (!Objects.equals(current.getEndTime(), incoming.endTime())) rejected.add("endTime");
        if (current.getNumberOfSessions() != incoming.numberOfSessions()) rejected.add("numberOfSessions");
        if (!Objects.equals(current.getRecurrenceType(), incoming.recurrenceType())) rejected.add("recurrenceType");

        if (tier == Tier.READ_ONLY) {
            // Cosmetic fields are also locked in READ_ONLY.
            if (!Objects.equals(current.getTitle(), incoming.title())) rejected.add("title");
            if (!Objects.equals(nullToEmpty(current.getDescription()), nullToEmpty(incoming.description())))
                rejected.add("description");
            if (!Objects.equals(nullToEmpty(current.getTeachers()), nullToEmpty(incoming.teachers())))
                rejected.add("teachers");
            if (!Objects.equals(current.getLocation(), incoming.location())) rejected.add("location");
            if (current.getMaxParticipants() != incoming.maxParticipants()) rejected.add("maxParticipants");
            if (!Objects.equals(current.getRoleBalanceThreshold(), incoming.roleBalanceThreshold()))
                rejected.add("roleBalanceThreshold");
        }
        return rejected;
    }

    private static boolean priceChanged(java.math.BigDecimal current, java.math.BigDecimal incoming) {
        if (current == null && incoming == null) return false;
        if (current == null || incoming == null) return true;
        return current.compareTo(incoming) != 0;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
