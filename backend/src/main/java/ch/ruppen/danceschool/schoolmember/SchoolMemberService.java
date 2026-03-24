package ch.ruppen.danceschool.schoolmember;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public SchoolMember createMembership(SchoolMember member) {
        return schoolMemberRepository.save(member);
    }
}
