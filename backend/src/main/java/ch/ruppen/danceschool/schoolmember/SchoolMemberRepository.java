package ch.ruppen.danceschool.schoolmember;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface SchoolMemberRepository extends JpaRepository<SchoolMember, Long> {

    List<SchoolMember> findByUserId(Long userId);

    Optional<SchoolMember> findByUserIdAndSchoolId(Long userId, Long schoolId);
}
