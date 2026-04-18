package ch.ruppen.danceschool.enrollment;

import ch.ruppen.danceschool.course.CourseLevel;

import java.time.Instant;

public record EnrollmentListDto(
        Long id,
        String studentName,
        String studentEmail,
        String studentPhoneNumber,
        DanceRole danceRole,
        EnrollmentStatus status,
        Instant enrolledAt,
        Instant approvedAt,
        Instant paidAt,
        Integer waitlistPosition,
        WaitlistReason waitlistReason,
        CourseLevel studentDanceLevel
) {
}
