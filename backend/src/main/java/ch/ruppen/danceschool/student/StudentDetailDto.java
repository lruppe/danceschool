package ch.ruppen.danceschool.student;

import ch.ruppen.danceschool.course.CourseLevel;
import ch.ruppen.danceschool.course.DanceStyle;

import java.util.List;

public record StudentDetailDto(
        Long id,
        String name,
        String email,
        String phoneNumber,
        List<DanceLevelDto> danceLevels
) {
    record DanceLevelDto(
            DanceStyle danceStyle,
            CourseLevel level
    ) {}
}
