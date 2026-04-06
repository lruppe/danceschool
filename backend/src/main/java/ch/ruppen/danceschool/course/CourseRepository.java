package ch.ruppen.danceschool.course;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findAllBySchoolId(Long schoolId);
}
