package ch.ruppen.danceschool.course;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record CourseListDto(
        Long id,
        String title,
        DanceStyle danceStyle,
        CourseLevel level,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int numberOfSessions,
        int enrolledStudents,
        int maxParticipants,
        BigDecimal price,
        CourseStatus status
) {
}
