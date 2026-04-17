package ch.ruppen.danceschool.schoolmember;

import ch.ruppen.danceschool.shared.logging.BusinessOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SchoolMemberService {

    private final SchoolMemberRepository schoolMemberRepository;

    public List<MembershipDto> findMembershipsByUserId(Long userId) {
        return schoolMemberRepository.findByUserId(userId).stream()
                .map(member -> new MembershipDto(
                        member.getSchool().getId(),
                        member.getSchool().getName(),
                        member.getRole()))
                .toList();
    }

    public Optional<SchoolMember> findByUserIdAndSchoolId(Long userId, Long schoolId) {
        return schoolMemberRepository.findByUserIdAndSchoolId(userId, schoolId);
    }

    @BusinessOperation(event = "MembershipCreated")
    public SchoolMember createMembership(SchoolMember member) {
        return schoolMemberRepository.save(member);
    }
}
