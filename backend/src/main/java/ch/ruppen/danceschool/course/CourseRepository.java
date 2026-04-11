package ch.ruppen.danceschool.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

interface CourseRepository extends JpaRepository<Course, Long> {

    List<Course> findAllBySchoolId(Long schoolId);

    boolean existsBySchoolId(Long schoolId);

    Optional<Course> findByIdAndSchoolId(Long id, Long schoolId);

    @Query("SELECT c FROM Course c WHERE c.school.id = :schoolId AND c.publishedAt IS NULL")
    List<Course> findDraftBySchoolId(Long schoolId);

    @Query("SELECT c FROM Course c WHERE c.school.id = :schoolId AND c.publishedAt IS NOT NULL AND c.startDate > :today")
    List<Course> findOpenBySchoolId(Long schoolId, LocalDate today);

    @Query("SELECT c FROM Course c WHERE c.school.id = :schoolId AND c.publishedAt IS NOT NULL AND c.startDate <= :today AND c.endDate >= :today")
    List<Course> findRunningBySchoolId(Long schoolId, LocalDate today);

    @Query("SELECT c FROM Course c WHERE c.school.id = :schoolId AND c.endDate < :today")
    List<Course> findFinishedBySchoolId(Long schoolId, LocalDate today);
}
