package ch.ruppen.danceschool.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findAllBySchoolId(Long schoolId);

    boolean existsBySchoolId(Long schoolId);

    Optional<Course> findByIdAndSchoolId(Long id, Long schoolId);
}
