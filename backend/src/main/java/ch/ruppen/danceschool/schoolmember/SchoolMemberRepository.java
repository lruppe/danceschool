package ch.ruppen.danceschool.schoolmember;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

interface SchoolMemberRepository extends JpaRepository<SchoolMember, Long> {

    List<SchoolMember> findByUserId(Long userId);
}
