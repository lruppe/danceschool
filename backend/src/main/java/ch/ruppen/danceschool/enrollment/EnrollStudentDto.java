package ch.ruppen.danceschool.enrollment;

import jakarta.validation.constraints.NotNull;

public record EnrollStudentDto(
        @NotNull Long studentId,
        DanceRole danceRole
) {
}
