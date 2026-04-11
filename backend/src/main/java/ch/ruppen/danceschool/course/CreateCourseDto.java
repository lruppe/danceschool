package ch.ruppen.danceschool.course;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record CreateCourseDto(
        @NotBlank @Size(max = 255) String title,
        @NotNull DanceStyle danceStyle,
        @NotNull CourseLevel level,
        @NotNull CourseType courseType,
        @Size(max = 4000) String description,
        @NotNull LocalDate startDate,
        @NotNull RecurrenceType recurrenceType,
        @Min(1) int numberOfSessions,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotBlank @Size(max = 255) String location,
        @Size(max = 255) String teachers,
        @Min(1) int maxParticipants,
        boolean roleBalancingEnabled,
        Integer roleBalanceThreshold,
        @NotNull PriceModel priceModel,
        @NotNull @DecimalMin("0") BigDecimal price,
        String status,
        LocalDate publishDate
) {
}
