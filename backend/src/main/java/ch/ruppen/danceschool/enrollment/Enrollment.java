package ch.ruppen.danceschool.enrollment;

import ch.ruppen.danceschool.course.Course;
import ch.ruppen.danceschool.schoolmember.SchoolMember;
import ch.ruppen.danceschool.student.Student;
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

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    private DanceRole danceRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status;

    @Column(nullable = false)
    private Instant enrolledAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrolled_by_member_id")
    private SchoolMember enrolledBy;

    private Instant approvedAt;

    private Instant paidAt;

    private Integer waitlistPosition;

    @Enumerated(EnumType.STRING)
    private WaitlistReason waitlistReason;
}
