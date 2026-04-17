package ch.ruppen.danceschool.enrollment;

public record EnrollmentResponseDto(
        Long enrollmentId,
        EnrollmentStatus status
) {
}
