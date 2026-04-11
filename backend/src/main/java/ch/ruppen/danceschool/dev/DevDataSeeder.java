package ch.ruppen.danceschool.dev;

import ch.ruppen.danceschool.course.CourseLevel;
import ch.ruppen.danceschool.course.CourseService;
import ch.ruppen.danceschool.course.CourseType;
import ch.ruppen.danceschool.course.CreateCourseDto;
import ch.ruppen.danceschool.course.DanceStyle;
import ch.ruppen.danceschool.course.PriceModel;
import ch.ruppen.danceschool.course.RecurrenceType;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

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
    private final CourseService courseService;

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

        seedCourses(owner, owner2);

        log.info("Dev data seeded: owner@test.com (School 1), owner2@test.com (School 2)");
    }

    private void seedCourses(AppUser owner, AppUser owner2) {
        LocalDate today = LocalDate.now();

        if (!courseService.hasCoursesForMember(owner.getId())) {
            courseService.seedCourse(owner.getId(), new CreateCourseDto(
                    "Salsa, Merengue, Bachata Solo", DanceStyle.SALSA, CourseLevel.BEGINNER,
                    CourseType.SOLO, "Learn the basics of Salsa, Merengue, and Bachata in solo style.",
                    LocalDate.of(2026, 4, 10), RecurrenceType.WEEKLY,
                    6, LocalTime.of(19, 30), LocalTime.of(20, 45),
                    "Studio A", "Maria", 12, false, null,
                    PriceModel.FIXED_COURSE, new BigDecimal("166.50"), null, null), 8, today);

            courseService.seedCourse(owner.getId(), new CreateCourseDto(
                    "Bachata Intermediate", DanceStyle.BACHATA, CourseLevel.INTERMEDIATE,
                    CourseType.PARTNER, "Take your Bachata to the next level with partner work and musicality.",
                    LocalDate.of(2026, 4, 7), RecurrenceType.WEEKLY,
                    8, LocalTime.of(19, 0), LocalTime.of(20, 0),
                    "Studio B", "Carlos", 15, true, 3,
                    PriceModel.FIXED_COURSE, new BigDecimal("220.00"), null, null), 12, today);

            courseService.seedCourse(owner.getId(), new CreateCourseDto(
                    "Salsa Advanced", DanceStyle.SALSA, CourseLevel.ADVANCED,
                    CourseType.PARTNER, "Advanced Salsa patterns, styling, and performance preparation.",
                    LocalDate.of(2026, 4, 8), RecurrenceType.WEEKLY,
                    10, LocalTime.of(20, 0), LocalTime.of(21, 15),
                    "Studio A", "Maria, Carlos", 10, true, 2,
                    PriceModel.FIXED_COURSE, new BigDecimal("310.00"), null, null), 10, today);

            courseService.seedCourse(owner.getId(), new CreateCourseDto(
                    "Bachata Beginners", DanceStyle.BACHATA, CourseLevel.BEGINNER,
                    CourseType.PARTNER, "Start your Bachata journey with the fundamentals of partner dancing.",
                    LocalDate.of(2026, 4, 6), RecurrenceType.WEEKLY,
                    6, LocalTime.of(18, 30), LocalTime.of(19, 45),
                    "Studio B", "Carlos", 16, true, null,
                    PriceModel.FIXED_COURSE, new BigDecimal("166.50"), null, null), 14, today);
        }

        if (!courseService.hasCoursesForMember(owner2.getId())) {
            courseService.seedCourse(owner2.getId(), new CreateCourseDto(
                    "Salsa Beginners", DanceStyle.SALSA, CourseLevel.BEGINNER,
                    CourseType.PARTNER, "Introduction to Salsa for complete beginners.",
                    LocalDate.of(2026, 4, 9), RecurrenceType.WEEKLY,
                    8, LocalTime.of(18, 0), LocalTime.of(19, 0),
                    "Main Hall", "Ana", 20, true, null,
                    PriceModel.FIXED_COURSE, new BigDecimal("180.00"), null, null), 5, today);

            courseService.seedCourse(owner2.getId(), new CreateCourseDto(
                    "Bachata Sensual", DanceStyle.BACHATA, CourseLevel.ADVANCED,
                    CourseType.PARTNER, "Explore Bachata Sensual technique and musicality.",
                    LocalDate.of(2026, 4, 11), RecurrenceType.WEEKLY,
                    6, LocalTime.of(14, 0), LocalTime.of(15, 30),
                    "Main Hall", "Ana, Luis", 12, true, null,
                    PriceModel.FIXED_COURSE, new BigDecimal("200.00"), null, null), 8, today);
        }
    }
}
