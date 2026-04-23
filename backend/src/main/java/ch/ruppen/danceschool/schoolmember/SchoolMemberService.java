package ch.ruppen.danceschool.schoolmember;

import ch.ruppen.danceschool.shared.error.DomainRuleViolationException;
import ch.ruppen.danceschool.shared.logging.BusinessOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SchoolMemberService {

    private final SchoolMemberRepository schoolMemberRepository;

    @Transactional(readOnly = true)
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

    public boolean existsByUserId(Long userId) {
        return schoolMemberRepository.existsByUserId(userId);
    }

    /**
     * Phase 1 assumes a single school per user (enforced by a unique constraint on
     * {@code school_member.user_id}). Phase 2 will revisit when users can belong to multiple
     * schools — this method will need to change shape accordingly.
     */
    @Transactional(readOnly = true)
    public Optional<Long> findSchoolIdByUserId(Long userId) {
        return schoolMemberRepository.findByUserId(userId).stream()
                .findFirst()
                .map(m -> m.getSchool().getId());
    }

    @BusinessOperation(event = "MembershipCreated")
    public SchoolMember createMembership(SchoolMember member) {
        if (existsByUserId(member.getUser().getId())) {
            throw new DomainRuleViolationException("User already belongs to a school");
        }
        return schoolMemberRepository.save(member);
    }
}
