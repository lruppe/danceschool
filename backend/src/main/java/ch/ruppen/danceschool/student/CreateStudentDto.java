package ch.ruppen.danceschool.student;

import ch.ruppen.danceschool.course.CourseLevel;
import ch.ruppen.danceschool.course.DanceStyle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateStudentDto(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 50) String phoneNumber,
        @Valid List<DanceLevelEntry> danceLevels
) {
    public record DanceLevelEntry(
            @NotNull DanceStyle danceStyle,
            @NotNull CourseLevel level
    ) {}
}
