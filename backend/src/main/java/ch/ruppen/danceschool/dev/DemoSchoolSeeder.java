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
import ch.ruppen.danceschool.user.UserCreatedEvent;
import ch.ruppen.danceschool.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates a fully populated dance school (profile, students, courses across all lifecycle states,
 * pending approvals, open and completed payments) for a given user. Used by:
 * <ul>
 *   <li>{@link DevDataSeeder} — to provision both dev owners on local startup.</li>
 *   <li>{@code UserService} — when {@code app.demo.enabled=true}, every newly created user gets
 *       their own demo school so the live deployment can act as a clickable demo.</li>
 * </ul>
 * Idempotent: if the user already owns a school, this does nothing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DemoSchoolSeeder {

    private final SchoolService schoolService;
    private final CourseService courseService;
    private final StudentService studentService;
    private final EnrollmentService enrollmentService;
    private final UserService userService;

    @Value("${app.demo.enabled:false}")
    private boolean demoEnabled;

    /**
     * Seeds a demo school for every brand-new user when {@code app.demo.enabled=true}.
     * The dev profile keeps this flag off and provisions its two fixed owners directly via
     * {@link DevDataSeeder}, so this listener is effectively a no-op there.
     * <p>
     * Runs after the user-creation transaction commits and in its own transaction so seeding
     * failures don't roll back the user — otherwise the next login would re-create the user,
     * re-trigger the same seed failure, and loop forever.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onUserCreated(UserCreatedEvent event) {
        if (!demoEnabled) {
            return;
        }
        userService.findById(event.userId())
                .ifPresent(user -> seedDemoSchoolFor(user, "Demo Dance School", "Zurich"));
    }

    @Transactional
    public void seedDemoSchoolFor(AppUser owner, String schoolName, String city) {
        if (schoolService.hasSchoolByMember(owner.getId())) {
            return;
        }

        SchoolUpdateDto schoolDto = new SchoolUpdateDto(
                schoolName,
                "Bachata · Salsa · Kizomba",
                "A demo school running on the Dance School platform. Click around — every screen is populated with realistic example data so you can explore the product.",
                "Bahnhofstrasse 1",
                city,
                "8001",
                "Switzerland",
                "+41 44 555 12 34",
                "info@" + slugify(schoolName) + ".example",
                null,
                null,
                null,
                List.of("Salsa", "Bachata", "Kizomba"),
                null,
                null
        );
        schoolService.createSchool(schoolDto, owner.getId());

        School school = schoolService.findSchoolByMember(owner.getId());
        List<Course> courses = seedCourses(owner);
        List<Student> students = seedStudents(owner, school);
        seedEnrollments(courses, students);

        log.info("Demo school seeded: name=\"{}\" userId={} courses={} students={}",
                schoolName, owner.getId(), courses.size(), students.size());
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
                "Studio A", "Maria", 12, null,
                PriceModel.FIXED_COURSE, new BigDecimal("166.50")), today.minusWeeks(2)));

        courses.add(courseService.seedCourse(owner.getId(), new CreateCourseDto(
                "Bachata Intermediate", DanceStyle.BACHATA, CourseLevel.INTERMEDIATE,
                CourseType.PARTNER, "Take your Bachata to the next level with partner work and musicality.",
                today.minusWeeks(2), RecurrenceType.WEEKLY,
                8, LocalTime.of(19, 0), LocalTime.of(20, 0),
                "Studio B", "Carlos", 15, 3,
                PriceModel.FIXED_COURSE, new BigDecimal("220.00")), today.minusWeeks(3)));

        // --- OPEN (PUBLISHED) courses — published, start in the future ---
        Course salsaAdvanced = courseService.seedCourse(owner.getId(), new CreateCourseDto(
                "Salsa Advanced", DanceStyle.SALSA, CourseLevel.ADVANCED,
                CourseType.PARTNER, "Advanced Salsa patterns, styling, and performance preparation.",
                today.plusWeeks(4), RecurrenceType.WEEKLY,
                10, LocalTime.of(20, 0), LocalTime.of(21, 15),
                "Studio A", "Maria, Carlos", 10, 2,
                PriceModel.FIXED_COURSE, new BigDecimal("310.00")), today.minusDays(5));
        courses.add(salsaAdvanced);

        courses.add(courseService.seedCourse(owner.getId(), new CreateCourseDto(
                "Bachata Beginners", DanceStyle.BACHATA, CourseLevel.BEGINNER,
                CourseType.PARTNER, "Start your Bachata journey with the fundamentals of partner dancing.",
                today.plusWeeks(5), RecurrenceType.WEEKLY,
                6, LocalTime.of(18, 30), LocalTime.of(19, 45),
                "Studio B", "Carlos", 16, 3,
                PriceModel.FIXED_COURSE, new BigDecimal("166.50")), today.minusDays(3)));

        // --- DRAFT courses (not published) ---
        courses.add(courseService.seedCourse(owner.getId(), new CreateCourseDto(
                "Salsa Social Dancing", DanceStyle.SALSA, CourseLevel.INTERMEDIATE,
                CourseType.PARTNER, "Social dancing techniques and floor craft.",
                today.plusWeeks(8), RecurrenceType.WEEKLY,
                8, LocalTime.of(19, 0), LocalTime.of(20, 0),
                "Studio A", "Maria", 20, null,
                PriceModel.FIXED_COURSE, new BigDecimal("220.00")), null));

        courses.add(courseService.seedCourse(owner.getId(), new CreateCourseDto(
                "Bachata Sensual", DanceStyle.BACHATA, CourseLevel.ADVANCED,
                CourseType.PARTNER, "Explore Bachata Sensual technique and musicality.",
                today.plusWeeks(10), RecurrenceType.WEEKLY,
                6, LocalTime.of(14, 0), LocalTime.of(15, 30),
                "Studio B", "Carlos, Ana", 12, 3,
                PriceModel.FIXED_COURSE, new BigDecimal("310.00")), null));

        // --- FINISHED (DONE) course — end date in the past ---
        courses.add(courseService.seedCourse(owner.getId(), new CreateCourseDto(
                "Kizomba Fundamentals", DanceStyle.KIZOMBA, CourseLevel.BEGINNER,
                CourseType.PARTNER, "Introduction to Kizomba connection and movement.",
                today.minusWeeks(12), RecurrenceType.WEEKLY,
                8, LocalTime.of(20, 0), LocalTime.of(21, 0),
                "Studio A", "Luis", 14, 3,
                PriceModel.FIXED_COURSE, new BigDecimal("180.00")), today.minusWeeks(14)));

        return courses;
    }

    private List<Student> seedStudents(AppUser owner, School school) {
        if (studentService.hasStudentsForSchool(owner.getId())) {
            return List.of();
        }

        List<Student> students = new ArrayList<>();
        students.add(studentService.seedStudent(school, "Anna Mueller", "anna.mueller@example.com", "+41 79 100 0001",
                List.of(new CreateStudentDto.DanceLevelEntry(DanceStyle.SALSA, CourseLevel.INTERMEDIATE),
                        new CreateStudentDto.DanceLevelEntry(DanceStyle.BACHATA, CourseLevel.BEGINNER))));

        students.add(studentService.seedStudent(school, "Marco Rossi", "marco.rossi@example.com", "+41 79 100 0002",
                List.of(new CreateStudentDto.DanceLevelEntry(DanceStyle.SALSA, CourseLevel.ADVANCED),
                        new CreateStudentDto.DanceLevelEntry(DanceStyle.BACHATA, CourseLevel.INTERMEDIATE),
                        new CreateStudentDto.DanceLevelEntry(DanceStyle.KIZOMBA, CourseLevel.BEGINNER))));

        students.add(studentService.seedStudent(school, "Laura Weber", "laura.weber@example.com", "+41 79 100 0003",
                List.of(new CreateStudentDto.DanceLevelEntry(DanceStyle.BACHATA, CourseLevel.ADVANCED))));

        students.add(studentService.seedStudent(school, "David Kim", "david.kim@example.com", null,
                List.of(new CreateStudentDto.DanceLevelEntry(DanceStyle.SALSA, CourseLevel.STARTER))));

        students.add(studentService.seedStudent(school, "Sofia Martinez", "sofia.martinez@example.com", "+41 79 100 0005",
                List.of(new CreateStudentDto.DanceLevelEntry(DanceStyle.SALSA, CourseLevel.BEGINNER),
                        new CreateStudentDto.DanceLevelEntry(DanceStyle.KIZOMBA, CourseLevel.INTERMEDIATE))));

        students.add(studentService.seedStudent(school, "Jan de Vries", "jan.devries@example.com", "+41 79 100 0006",
                List.of()));

        students.add(studentService.seedStudent(school, "Yuki Tanaka", "yuki.tanaka@example.com", "+41 79 100 0007",
                List.of(new CreateStudentDto.DanceLevelEntry(DanceStyle.SALSA, CourseLevel.INTERMEDIATE),
                        new CreateStudentDto.DanceLevelEntry(DanceStyle.BACHATA, CourseLevel.INTERMEDIATE),
                        new CreateStudentDto.DanceLevelEntry(DanceStyle.ZOUK, CourseLevel.BEGINNER))));

        return students;
    }

    private void seedEnrollments(List<Course> courses, List<Student> students) {
        if (courses.isEmpty() || students.isEmpty()) {
            return;
        }

        Instant now = Instant.now();

        // courses[0] = Salsa Solo (SOLO, BEGINNER, max 12)
        // Pending approvals here so the first course in the list shows clickable approval rows.
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

        // courses[1] = Bachata Intermediate (PARTNER)
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

        // courses[2] = Salsa Advanced (PARTNER, ADVANCED) — pending approvals (level mismatch)
        Course salsaAdvanced = courses.get(2);
        enrollmentService.seedEnrollment(salsaAdvanced, students.get(0), DanceRole.FOLLOW,
                EnrollmentStatus.PENDING_APPROVAL, now.minus(1, ChronoUnit.DAYS), null);
        enrollmentService.seedEnrollment(salsaAdvanced, students.get(5), DanceRole.LEAD,
                EnrollmentStatus.PENDING_APPROVAL, now.minus(12, ChronoUnit.HOURS), null);

        // courses[3] = Bachata Beginners — mix of paid (completed) and unpaid (open) for the Payments page
        Course bachataBeginners = courses.get(3);
        enrollmentService.seedEnrollment(bachataBeginners, students.get(2), DanceRole.FOLLOW,
                EnrollmentStatus.CONFIRMED, now.minus(6, ChronoUnit.DAYS), now.minus(4, ChronoUnit.DAYS));
        enrollmentService.seedEnrollment(bachataBeginners, students.get(3), DanceRole.LEAD,
                EnrollmentStatus.CONFIRMED, now.minus(5, ChronoUnit.DAYS), now.minus(3, ChronoUnit.DAYS));
        enrollmentService.seedEnrollment(bachataBeginners, students.get(0), DanceRole.FOLLOW,
                EnrollmentStatus.PENDING_PAYMENT, now.minus(4, ChronoUnit.DAYS), null);
        enrollmentService.seedEnrollment(bachataBeginners, students.get(1), DanceRole.LEAD,
                EnrollmentStatus.PENDING_PAYMENT, now.minus(1, ChronoUnit.DAYS), null);
        enrollmentService.seedEnrollment(bachataBeginners, students.get(5), DanceRole.LEAD,
                EnrollmentStatus.PENDING_PAYMENT, now.minus(6, ChronoUnit.HOURS), null);
    }

    private static String slugify(String s) {
        return s.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }
}
