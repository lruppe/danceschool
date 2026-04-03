package ch.ruppen.danceschool.school;

import ch.ruppen.danceschool.schoolmember.MemberRole;
import ch.ruppen.danceschool.schoolmember.SchoolMember;
import ch.ruppen.danceschool.schoolmember.SchoolMemberService;
import ch.ruppen.danceschool.shared.error.ResourceNotFoundException;
import ch.ruppen.danceschool.user.AppUser;
import ch.ruppen.danceschool.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CreateSchoolUseCase {

    private final SchoolService schoolService;
    private final SchoolMemberService schoolMemberService;
    private final UserService userService;

    @Transactional
    public SchoolDetailDto execute(SchoolUpdateDto dto, Long userId) {
        AppUser user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        School school = schoolService.createSchoolFull(dto);

        SchoolMember member = new SchoolMember();
        member.setUser(user);
        member.setSchool(school);
        member.setRole(MemberRole.OWNER);
        schoolMemberService.createMembership(member);

        return schoolService.toDetailDto(school);
    }
}
