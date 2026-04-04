package ch.ruppen.danceschool.shared.security;

import ch.ruppen.danceschool.school.SchoolService;
import ch.ruppen.danceschool.school.SchoolUpdateDto;
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

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppUser owner = userService.findOrCreateByFirebaseUid("dev-owner", "owner@test.com", "Dev Owner");
        AppUser owner2 = userService.findOrCreateByFirebaseUid("dev-owner-2", "owner2@test.com", "Dev Owner 2");

        if (!schoolService.hasSchoolByMember(owner.getId())) {
            var schoolDto = new SchoolUpdateDto("Dev Dance School", null, null, null, "Zurich",
                    null, "Switzerland", null, "info@devdanceschool.com", null, null, null,
                    null, null, null);
            schoolService.createSchool(schoolDto, owner.getId());
        }

        if (!schoolService.hasSchoolByMember(owner2.getId())) {
            var school2Dto = new SchoolUpdateDto("Other Dance School", null, null, null, "Bern",
                    null, "Switzerland", null, "info@otherdanceschool.com", null, null, null,
                    null, null, null);
            schoolService.createSchool(school2Dto, owner2.getId());
        }

        log.info("Dev data seeded: owner@test.com (School 1), owner2@test.com (School 2)");
    }
}
