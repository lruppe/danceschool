package ch.ruppen.danceschool.course;

import ch.ruppen.danceschool.school.School;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    private String title;

    @Enumerated(EnumType.STRING)
    private DanceStyle danceStyle;

    @Enumerated(EnumType.STRING)
    private CourseLevel level;

    @Enumerated(EnumType.STRING)
    private CourseType courseType;

    @Column(columnDefinition = "TEXT")
    private String description;

    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    private RecurrenceType recurrenceType;

    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    private int numberOfSessions;

    private LocalTime startTime;

    private LocalTime endTime;

    private String location;

    private String teachers;

    private int maxParticipants;

    private boolean waitingListEnabled;

    private String enrollmentOpenDate;

    private String enrollmentCloseDate;

    private boolean requireRoleSelection;

    @Enumerated(EnumType.STRING)
    private RoleBalancingMode roleBalancingMode;

    private Integer roleBalanceThreshold;

    @Enumerated(EnumType.STRING)
    private PriceModel priceModel;

    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private CourseStatus status;

    private LocalDate publishDate;

    private int enrolledStudents;
}
