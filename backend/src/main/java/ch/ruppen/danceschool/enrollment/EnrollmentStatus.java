package ch.ruppen.danceschool.enrollment;

import java.util.List;

public enum EnrollmentStatus {
    PENDING_APPROVAL,
    PENDING_PAYMENT,
    CONFIRMED,
    WAITLISTED,
    REJECTED;

    /**
     * Enrollments in these statuses hold a seat — they count toward the course's capacity,
     * role-balance check, and the `enrolledStudents` total exposed in DTOs.
     * PENDING_APPROVAL does not reserve a seat until approved; WAITLISTED / REJECTED never do.
     */
    public static final List<EnrollmentStatus> SEAT_HOLDING_STATI = List.of(PENDING_PAYMENT, CONFIRMED);
}
