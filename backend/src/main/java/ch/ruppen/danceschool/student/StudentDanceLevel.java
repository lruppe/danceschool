package ch.ruppen.danceschool.student;

import ch.ruppen.danceschool.course.CourseLevel;
import ch.ruppen.danceschool.course.DanceStyle;
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
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "student_dance_level")
@Getter
@Setter
@NoArgsConstructor
public class StudentDanceLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "dance_style", nullable = false)
    private DanceStyle danceStyle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseLevel level;
}
