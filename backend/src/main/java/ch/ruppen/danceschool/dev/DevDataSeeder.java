package ch.ruppen.danceschool.dev;

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
 * Seeds development users and their schools on startup so that local dev login
 * lands directly in the app shell (no onboarding step).
 * <p>
 * Only active when {@code app.security.dev-auth} is {@code true}. Actual school content
 * is provisioned by {@link DemoSchoolSeeder} — the same component that runs in demo mode
 * for new Firebase-authenticated users.
 */
@Component
@ConditionalOnProperty(name = "app.security.dev-auth", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DevDataSeeder implements ApplicationRunner {

    private final UserService userService;
    private final DemoSchoolSeeder demoSchoolSeeder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppUser owner = userService.findOrCreateByFirebaseUid("dev-owner", "owner@test.com", "Dev Owner");
        AppUser owner2 = userService.findOrCreateByFirebaseUid("dev-owner-2", "owner2@test.com", "Dev Owner 2");

        demoSchoolSeeder.seedDemoSchoolFor(owner, "Dev Dance School", "Zurich");
        demoSchoolSeeder.seedDemoSchoolFor(owner2, "Other Dance School", "Bern");

        log.info("Dev data seeded: owner@test.com (School 1), owner2@test.com (School 2)");
    }
}
