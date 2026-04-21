package ch.ruppen.danceschool.student;

import ch.ruppen.danceschool.enrollment.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByIdAndSchoolId(Long id, Long schoolId);

    boolean existsBySchoolId(Long schoolId);

    /**
     * Returns students for a school along with the count of their "active" courses —
     * seat-holding enrollments in courses that are published and not yet finished
     * (covers both OPEN, i.e. published with a future start date, and RUNNING).
     */
    @Query("""
            SELECT new ch.ruppen.danceschool.student.StudentListDto(
                s.id, s.name, s.email, s.phoneNumber,
                COUNT(DISTINCT CASE WHEN c.publishedAt IS NOT NULL
                                     AND c.endDate   >= :today
                                    THEN c.id END)
            )
            FROM Student s
            LEFT JOIN Enrollment e ON e.student = s AND e.status IN :statuses
            LEFT JOIN e.course c
            WHERE s.school.id = :schoolId
            GROUP BY s.id, s.name, s.email, s.phoneNumber
            ORDER BY s.name ASC
            """)
    List<StudentListDto> findListBySchoolId(@Param("schoolId") Long schoolId,
                                            @Param("statuses") List<EnrollmentStatus> statuses,
                                            @Param("today") LocalDate today);
}
