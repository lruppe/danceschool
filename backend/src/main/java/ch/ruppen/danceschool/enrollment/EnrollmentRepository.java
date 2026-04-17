package ch.ruppen.danceschool.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    List<Enrollment> findAllByCourseId(Long courseId);

    Optional<Enrollment> findByIdAndCourseSchoolId(Long id, Long schoolId);

    long countByCourseIdAndStatusIn(Long courseId, List<EnrollmentStatus> statuses);

    boolean existsByStudentIdAndCourseIdAndStatusNot(Long studentId, Long courseId, EnrollmentStatus status);
}
