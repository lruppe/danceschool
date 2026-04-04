package ch.ruppen.danceschool.shared.security;

import ch.ruppen.danceschool.school.School;
import ch.ruppen.danceschool.school.SchoolService;
import ch.ruppen.danceschool.school.SchoolUpdateDto;
import ch.ruppen.danceschool.schoolmember.MemberRole;
import ch.ruppen.danceschool.schoolmember.SchoolMember;
import ch.ruppen.danceschool.schoolmember.SchoolMemberService;
import ch.ruppen.danceschool.user.AppUser;
import ch.ruppen.danceschool.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds development users and a school on startup so that local dev login
 * lands directly in the app shell (no onboarding step).
 * <p>
 * Only active when {@code app.security.dev-auth} is {@code true}.
 */
@Component
@ConditionalOnProperty(name = "app.security.dev-auth", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder implements ApplicationRunner {

    private final UserService userService;
    private final SchoolService schoolService;
    private final SchoolMemberService schoolMemberService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppUser owner = userService.findOrCreateByFirebaseUid("dev-owner", "owner@test.com", "Dev Owner");
        AppUser user = userService.findOrCreateByFirebaseUid("dev-user", "user@test.com", "Dev User");

        if (!schoolService.hasSchoolByOwner(owner.getId())) {
            var schoolDto = new SchoolUpdateDto("Dev Dance School", null, null, null, "Zurich",
                    null, "Switzerland", null, "info@devdanceschool.com", null, null, null,
                    null, null, null);
            schoolService.createSchool(schoolDto, owner.getId());

            School school = schoolService.findSchoolByOwner(owner.getId());
            SchoolMember member = new SchoolMember();
            member.setUser(user);
            member.setSchool(school);
            member.setRole(MemberRole.USER);
            schoolMemberService.createMembership(member);
        }

        log.info("Dev data seeded: owner@test.com (OWNER), user@test.com (USER)");
    }
}
