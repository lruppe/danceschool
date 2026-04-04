package ch.ruppen.danceschool.school;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

interface SchoolRepository extends JpaRepository<School, Long> {

    @Query("SELECT s FROM School s JOIN SchoolMember m ON m.school = s WHERE m.user.id = :userId")
    Optional<School> findByMemberUserId(Long userId);
}
