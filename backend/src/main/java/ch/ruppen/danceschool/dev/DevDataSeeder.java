package ch.ruppen.danceschool.dev;

import ch.ruppen.danceschool.course.Course;
import ch.ruppen.danceschool.course.CourseLevel;
import ch.ruppen.danceschool.course.CourseService;
import ch.ruppen.danceschool.course.CourseType;
import ch.ruppen.danceschool.course.CreateCourseDto;
import ch.ruppen.danceschool.course.DanceStyle;
import ch.ruppen.danceschool.course.PriceModel;
import ch.ruppen.danceschool.course.RecurrenceType;
import ch.ruppen.danceschool.enrollment.DanceRole;
import ch.ruppen.danceschool.enrollment.EnrollmentService;
import ch.ruppen.danceschool.enrollment.EnrollmentStatus;
import ch.ruppen.danceschool.school.School;
import ch.ruppen.danceschool.school.SchoolService;
import ch.ruppen.danceschool.school.SchoolUpdateDto;
import ch.ruppen.danceschool.student.CreateStudentDto;
import ch.ruppen.danceschool.student.Student;
import ch.ruppen.danceschool.student.StudentService;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

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
    private final StudentService studentService;
    private final EnrollmentService enrollmentService;

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

        List<Course> courses1 = seedCourses(owner);
        List<Course> courses2 = seedCoursesForOwner2(owner2);
        List<Student> students1 = seedStudents(owner, owner2);
        seedEnrollments(owner, courses1, students1);

        log.info("Dev data seeded: owner@test.com (School 1), owner2@test.com (School 2)");
    }

    private List<Course> seedCourses(AppUser owner) {
        LocalDate today = LocalDate.now();

        if (courseService.hasCoursesForMember(owner.getId())) {
            return List.of();
        }

        List<Course> courses = new ArrayList<>();

        // --- RUNNING courses (published, start in the past, end in the future) ---
        courses.add(courseService.seedCourse(owner.getId(), new CreateCourseDto(
                "Salsa, Merengue, Bachata Solo", DanceStyle.SALSA, CourseLevel.BEGINNER,
                CourseType.SOLO, "Learn the basics of Salsa, Merengue, and Bachata in solo style.",
                today.minusWeeks(1), RecurrenceType.WEEKLY,
                6, LocalTime.of(19, 30), LocalTime.of(20, 45),
                "Studio A", "Maria", 12, false, null,
                PriceModel.FIXED_COURSE, new BigDecimal("166.50")), 0, today.minusWeeks(2)));

        courses.add(courseService.seedCourse(owner.getId(), new CreateCourseDto(
                "Bachata Intermediate", DanceStyle.BACHATA, CourseLevel.INTERMEDIATE,
                CourseType.PARTNER, "Take your Bachata to the next level with partner work and musicality.",
                today.minusWeeks(2), RecurrenceType.WEEKLY,
                8, LocalTime.of(19, 0), LocalTime.of(20, 0),
                "Studio B", "Carlos", 15, true, 3,
                PriceModel.FIXED_COURSE, new BigDecimal("220.00")), 0, today.minusWeeks(3)));

        // --- OPEN courses (published, start in the future) ---
        courses.add(courseService.seedCourse(owner.getId(), new CreateCourseDto(
                "Salsa Advanced", DanceStyle.SALSA, CourseLevel.ADVANCED,
                CourseType.PARTNER, "Advanced Salsa patterns, styling, and performance preparation.",
                today.plusWeeks(4), RecurrenceType.WEEKLY,
                10, LocalTime.of(20, 0), LocalTime.of(21, 15),
                "Studio A", "Maria, Carlos", 10, true, 2,
                PriceModel.FIXED_COURSE, new BigDecimal("310.00")), 0, today.minusDays(5)));

        courses.add(courseService.seedCourse(owner.getId(), new CreateCourseDto(
                "Bachata Beginners", DanceStyle.BACHATA, CourseLevel.BEGINNER,
                CourseType.PARTNER, "Start your Bachata journey with the fundamentals of partner dancing.",
                today.plusWeeks(5), RecurrenceType.WEEKLY,
                6, LocalTime.of(18, 30), LocalTime.of(19, 45),
                "Studio B", "Carlos", 16, true, null,
                PriceModel.FIXED_COURSE, new BigDecimal("166.50")), 0, today.minusDays(3)));

        // --- DRAFT courses (not published) ---
        courses.add(courseService.seedCourse(owner.getId(), new CreateCourseDto(
                "Salsa Social Dancing", DanceStyle.SALSA, CourseLevel.INTERMEDIATE,
                CourseType.PARTNER, "Social dancing techniques and floor craft.",
                today.plusWeeks(8), RecurrenceType.WEEKLY,
                8, LocalTime.of(19, 0), LocalTime.of(20, 0),
                "Studio A", "Maria", 20, false, null,
                PriceModel.FIXED_COURSE, new BigDecimal("220.00")), 0, null));

        courses.add(courseService.seedCourse(owner.getId(), new CreateCourseDto(
                "Bachata Sensual", DanceStyle.BACHATA, CourseLevel.ADVANCED,
                CourseType.PARTNER, "Explore Bachata Sensual technique and musicality.",
                today.plusWeeks(10), RecurrenceType.WEEKLY,
                6, LocalTime.of(14, 0), LocalTime.of(15, 30),
                "Studio B", "Carlos, Ana", 12, true, null,
                PriceModel.FIXED_COURSE, new BigDecimal("310.00")), 0, null));

        // --- FINISHED course (end date in the past) ---
        courses.add(courseService.seedCourse(owner.getId(), new CreateCourseDto(
                "Kizomba Fundamentals", DanceStyle.KIZOMBA, CourseLevel.BEGINNER,
                CourseType.PARTNER, "Introduction to Kizomba connection and movement.",
                today.minusWeeks(12), RecurrenceType.WEEKLY,
                8, LocalTime.of(20, 0), LocalTime.of(21, 0),
                "Studio A", "Luis", 14, true, null,
                PriceModel.FIXED_COURSE, new BigDecimal("180.00")), 0, today.minusWeeks(14)));

        return courses;
    }

    private List<Course> seedCoursesForOwner2(AppUser owner2) {
        LocalDate today = LocalDate.now();

        if (courseService.hasCoursesForMember(owner2.getId())) {
            return List.of();
        }

        List<Course> courses = new ArrayList<>();

        courses.add(courseService.seedCourse(owner2.getId(), new CreateCourseDto(
                "Salsa Beginners", DanceStyle.SALSA, CourseLevel.BEGINNER,
                CourseType.PARTNER, "Introduction to Salsa for complete beginners.",
                today.minusWeeks(1), RecurrenceType.WEEKLY,
                8, LocalTime.of(18, 0), LocalTime.of(19, 0),
                "Main Hall", "Ana", 20, true, null,
                PriceModel.FIXED_COURSE, new BigDecimal("180.00")), 0, today.minusWeeks(2)));

        courses.add(courseService.seedCourse(owner2.getId(), new CreateCourseDto(
                "Bachata Sensual", DanceStyle.BACHATA, CourseLevel.ADVANCED,
                CourseType.PARTNER, "Explore Bachata Sensual technique and musicality.",
                today.plusWeeks(3), RecurrenceType.WEEKLY,
                6, LocalTime.of(14, 0), LocalTime.of(15, 30),
                "Main Hall", "Ana, Luis", 12, true, null,
                PriceModel.FIXED_COURSE, new BigDecimal("200.00")), 0, today.minusDays(2)));

        return courses;
    }

    private List<Student> seedStudents(AppUser owner, AppUser owner2) {
        if (studentService.hasStudentsForSchool(owner.getId())) {
            return List.of();
        }

        School school1 = schoolService.findSchoolByMember(owner.getId());
        School school2 = schoolService.findSchoolByMember(owner2.getId());

        List<Student> students = new ArrayList<>();

        // School 1: 7 students with varied dance levels
        students.add(studentService.seedStudent(school1, "Anna Mueller", "anna.mueller@example.com", "+41 79 100 0001",
                List.of(new CreateStudentDto.DanceLevelEntry(DanceStyle.SALSA, CourseLevel.INTERMEDIATE),
                        new CreateStudentDto.DanceLevelEntry(DanceStyle.BACHATA, CourseLevel.BEGINNER))));

        students.add(studentService.seedStudent(school1, "Marco Rossi", "marco.rossi@example.com", "+41 79 100 0002",
                List.of(new CreateStudentDto.DanceLevelEntry(DanceStyle.SALSA, CourseLevel.ADVANCED),
                        new CreateStudentDto.DanceLevelEntry(DanceStyle.BACHATA, CourseLevel.INTERMEDIATE),
                        new CreateStudentDto.DanceLevelEntry(DanceStyle.KIZOMBA, CourseLevel.BEGINNER))));

        students.add(studentService.seedStudent(school1, "Laura Weber", "laura.weber@example.com", "+41 79 100 0003",
                List.of(new CreateStudentDto.DanceLevelEntry(DanceStyle.BACHATA, CourseLevel.ADVANCED))));

        students.add(studentService.seedStudent(school1, "David Kim", "david.kim@example.com", null,
                List.of(new CreateStudentDto.DanceLevelEntry(DanceStyle.SALSA, CourseLevel.STARTER))));

        students.add(studentService.seedStudent(school1, "Sofia Martinez", "sofia.martinez@example.com", "+41 79 100 0005",
                List.of(new CreateStudentDto.DanceLevelEntry(DanceStyle.SALSA, CourseLevel.BEGINNER),
                        new CreateStudentDto.DanceLevelEntry(DanceStyle.KIZOMBA, CourseLevel.INTERMEDIATE))));

        students.add(studentService.seedStudent(school1, "Jan de Vries", "jan.devries@example.com", "+41 79 100 0006",
                List.of()));

        students.add(studentService.seedStudent(school1, "Yuki Tanaka", "yuki.tanaka@example.com", "+41 79 100 0007",
                List.of(new CreateStudentDto.DanceLevelEntry(DanceStyle.SALSA, CourseLevel.INTERMEDIATE),
                        new CreateStudentDto.DanceLevelEntry(DanceStyle.BACHATA, CourseLevel.INTERMEDIATE),
                        new CreateStudentDto.DanceLevelEntry(DanceStyle.ZOUK, CourseLevel.BEGINNER))));

        // School 2: 3 students
        studentService.seedStudent(school2, "Elena Fischer", "elena.fischer@example.com", "+41 79 200 0001",
                List.of(new CreateStudentDto.DanceLevelEntry(DanceStyle.SALSA, CourseLevel.BEGINNER)));

        studentService.seedStudent(school2, "Thomas Bauer", "thomas.bauer@example.com", "+41 79 200 0002",
                List.of(new CreateStudentDto.DanceLevelEntry(DanceStyle.BACHATA, CourseLevel.INTERMEDIATE),
                        new CreateStudentDto.DanceLevelEntry(DanceStyle.SALSA, CourseLevel.INTERMEDIATE)));

        studentService.seedStudent(school2, "Mia Schmidt", "mia.schmidt@example.com", null,
                List.of(new CreateStudentDto.DanceLevelEntry(DanceStyle.SALSA, CourseLevel.STARTER)));

        return students;
    }

    private void seedEnrollments(AppUser owner, List<Course> courses, List<Student> students) {
        if (courses.isEmpty() || students.isEmpty()) {
            return;
        }

        Instant now = Instant.now();

        // courses[0] = "Salsa, Merengue, Bachata Solo" (SOLO, BEGINNER, max 12)
        // Enroll 4 students as CONFIRMED, 2 as PENDING_PAYMENT
        Course soloCourse = courses.get(0);
        enrollmentService.seedEnrollment(soloCourse, students.get(0), null,
                EnrollmentStatus.CONFIRMED, now.minus(10, ChronoUnit.DAYS), now.minus(8, ChronoUnit.DAYS));
        enrollmentService.seedEnrollment(soloCourse, students.get(1), null,
                EnrollmentStatus.CONFIRMED, now.minus(9, ChronoUnit.DAYS), now.minus(7, ChronoUnit.DAYS));
        enrollmentService.seedEnrollment(soloCourse, students.get(3), null,
                EnrollmentStatus.CONFIRMED, now.minus(8, ChronoUnit.DAYS), now.minus(6, ChronoUnit.DAYS));
        enrollmentService.seedEnrollment(soloCourse, students.get(4), null,
                EnrollmentStatus.CONFIRMED, now.minus(7, ChronoUnit.DAYS), now.minus(5, ChronoUnit.DAYS));
        enrollmentService.seedEnrollment(soloCourse, students.get(5), null,
                EnrollmentStatus.PENDING_PAYMENT, now.minus(3, ChronoUnit.DAYS), null);
        enrollmentService.seedEnrollment(soloCourse, students.get(6), null,
                EnrollmentStatus.PENDING_PAYMENT, now.minus(2, ChronoUnit.DAYS), null);
        soloCourse.setEnrolledStudents(6);

        // courses[1] = "Bachata Intermediate" (PARTNER, INTERMEDIATE, max 15, roleBalancing=true, threshold=3)
        // Enroll 5 students with LEAD/FOLLOW roles, mix of CONFIRMED and PENDING_PAYMENT
        Course partnerCourse = courses.get(1);
        enrollmentService.seedEnrollment(partnerCourse, students.get(0), DanceRole.FOLLOW,
                EnrollmentStatus.CONFIRMED, now.minus(12, ChronoUnit.DAYS), now.minus(10, ChronoUnit.DAYS));
        enrollmentService.seedEnrollment(partnerCourse, students.get(1), DanceRole.LEAD,
                EnrollmentStatus.CONFIRMED, now.minus(11, ChronoUnit.DAYS), now.minus(9, ChronoUnit.DAYS));
        enrollmentService.seedEnrollment(partnerCourse, students.get(2), DanceRole.FOLLOW,
                EnrollmentStatus.CONFIRMED, now.minus(10, ChronoUnit.DAYS), now.minus(8, ChronoUnit.DAYS));
        enrollmentService.seedEnrollment(partnerCourse, students.get(6), DanceRole.LEAD,
                EnrollmentStatus.CONFIRMED, now.minus(9, ChronoUnit.DAYS), now.minus(7, ChronoUnit.DAYS));
        enrollmentService.seedEnrollment(partnerCourse, students.get(4), DanceRole.FOLLOW,
                EnrollmentStatus.PENDING_PAYMENT, now.minus(2, ChronoUnit.DAYS), null);
        partnerCourse.setEnrolledStudents(5);
    }
}
