package ch.ruppen.danceschool.student;

import ch.ruppen.danceschool.course.CourseLevel;
import ch.ruppen.danceschool.course.DanceStyle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateDanceLevelsDto(
        @Valid @NotNull List<DanceLevelEntry> danceLevels
) {
    record DanceLevelEntry(
            @NotNull DanceStyle danceStyle,
            @NotNull CourseLevel level
    ) {}
}
