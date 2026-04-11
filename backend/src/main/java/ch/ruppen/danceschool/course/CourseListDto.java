package ch.ruppen.danceschool.course;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
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
        LocalDate endDate,
        int enrolledStudents,
        int maxParticipants,
        BigDecimal price,
        CourseStatus status
) {
}
