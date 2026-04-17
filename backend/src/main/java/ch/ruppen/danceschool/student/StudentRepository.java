package ch.ruppen.danceschool.student;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface StudentRepository extends JpaRepository<Student, Long> {

    List<Student> findAllBySchoolId(Long schoolId);

    Optional<Student> findByIdAndSchoolId(Long id, Long schoolId);

    boolean existsBySchoolId(Long schoolId);
}
