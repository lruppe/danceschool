package ch.ruppen.danceschool.course;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public record CourseDetailDto(
        Long id,
        String title,
        DanceStyle danceStyle,
        CourseLevel level,
        CourseType courseType,
        String description,
        LocalDate startDate,
        RecurrenceType recurrenceType,
        DayOfWeek dayOfWeek,
        int numberOfSessions,
        LocalTime startTime,
        LocalTime endTime,
        String location,
        String teachers,
        int maxParticipants,
        boolean waitingListEnabled,
        boolean requireRoleSelection,
        RoleBalancingMode roleBalancingMode,
        Integer roleBalanceThreshold,
        PriceModel priceModel,
        BigDecimal price,
        CourseStatus status,
        LocalDate publishDate,
        int enrolledStudents
) {
}
