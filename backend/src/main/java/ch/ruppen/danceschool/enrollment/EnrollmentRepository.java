package ch.ruppen.danceschool.enrollment;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    // Kills the 1 + 2N lazy loads in EnrollmentService.toListDto (student fields,
    // student.danceLevels, course.danceStyle). "course" is sufficient because its
    // basic columns (including danceStyle) are eager once the entity is loaded.
    @EntityGraph(attributePaths = {"student", "student.danceLevels", "course"})
    List<Enrollment> findAllByCourseId(Long courseId);

    Optional<Enrollment> findByIdAndCourseSchoolId(Long id, Long schoolId);

    long countByCourseIdAndStatusIn(Long courseId, List<EnrollmentStatus> statuses);

    long countByCourseIdAndDanceRoleAndStatusIn(Long courseId, DanceRole danceRole, List<EnrollmentStatus> statuses);

    long countByCourseIdAndStatus(Long courseId, EnrollmentStatus status);

    long countByCourseIdAndStatusAndDanceRole(Long courseId, EnrollmentStatus status, DanceRole danceRole);

    boolean existsByStudentIdAndCourseIdAndStatusNot(Long studentId, Long courseId, EnrollmentStatus status);

    @Query("SELECT e.course.id, e.danceRole, COUNT(e) FROM Enrollment e " +
            "WHERE e.course.id IN :courseIds AND e.status IN :statuses " +
            "GROUP BY e.course.id, e.danceRole")
    List<Object[]> countByRoleGroupedByCourse(@Param("courseIds") List<Long> courseIds,
                                              @Param("statuses") List<EnrollmentStatus> statuses);

    @Query("SELECT e.course.id, COUNT(e) FROM Enrollment e " +
            "WHERE e.course.id IN :courseIds AND e.status IN :statuses " +
            "GROUP BY e.course.id")
    List<Object[]> countGroupedByCourse(@Param("courseIds") List<Long> courseIds,
                                        @Param("statuses") List<EnrollmentStatus> statuses);
}
