package ch.ruppen.danceschool.course;

import ch.ruppen.danceschool.school.School;
import ch.ruppen.danceschool.school.SchoolService;
import ch.ruppen.danceschool.shared.error.DomainRuleViolationException;
import ch.ruppen.danceschool.shared.error.ResourceNotFoundException;
import ch.ruppen.danceschool.shared.logging.BusinessOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final SchoolService schoolService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<CourseListDto> getCoursesByMember(Long userId, CourseLifecycleStatus statusFilter) {
        School school = schoolService.findSchoolByMember(userId);
        List<Course> courses = fetchCourses(school.getId(), statusFilter);
        LocalDate today = LocalDate.now(clock);
        return courses.stream()
                .map(c -> toListDto(c, today))
                .toList();
    }

    @Transactional
    @BusinessOperation(event = "CourseCreated")
    public Long createCourse(Long userId, CreateCourseDto dto) {
        validateDomainRules(dto, true);
        School school = schoolService.findSchoolByMember(userId);
        Course course = new Course();
        course.setSchool(school);
        applyDto(course, dto);
        courseRepository.save(course);
        return course.getId();
    }

    @Transactional
    @BusinessOperation(event = "CourseUpdated")
    public CourseDetailDto updateCourse(Long userId, Long courseId, CreateCourseDto dto) {
        validateDomainRules(dto, false);
        School school = schoolService.findSchoolByMember(userId);
        Course course = courseRepository.findByIdAndSchoolId(courseId, school.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
        applyDto(course, dto);
        return toDetailDto(course, LocalDate.now(clock));
    }

    @Transactional(readOnly = true)
    public CourseDetailDto getCourseDetail(Long userId, Long courseId) {
        School school = schoolService.findSchoolByMember(userId);
        Course course = courseRepository.findByIdAndSchoolId(courseId, school.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
        return toDetailDto(course, LocalDate.now(clock));
    }

    /**
     * Seeds a course for dev/test data. Skips domain validation (seed data may have past dates).
     */
    @Transactional
    public void seedCourse(Long userId, CreateCourseDto dto, int enrolledStudents, LocalDate publishedAt) {
        School school = schoolService.findSchoolByMember(userId);
        Course course = new Course();
        course.setSchool(school);
        applyDto(course, dto);
        course.setEnrolledStudents(enrolledStudents);
        course.setPublishedAt(publishedAt);
        courseRepository.save(course);
    }

    @Transactional(readOnly = true)
    public boolean hasCoursesForMember(Long userId) {
        School school = schoolService.findSchoolByMember(userId);
        return courseRepository.existsBySchoolId(school.getId());
    }

    private List<Course> fetchCourses(Long schoolId, CourseLifecycleStatus statusFilter) {
        if (statusFilter == null) {
            return courseRepository.findAllBySchoolId(schoolId);
        }
        LocalDate today = LocalDate.now(clock);
        return switch (statusFilter) {
            case DRAFT -> courseRepository.findDraftBySchoolId(schoolId);
            case OPEN -> courseRepository.findOpenBySchoolId(schoolId, today);
            case RUNNING -> courseRepository.findRunningBySchoolId(schoolId, today);
            case FINISHED -> courseRepository.findFinishedBySchoolId(schoolId, today);
        };
    }

    private void validateDomainRules(CreateCourseDto dto, boolean isCreate) {
        if (!dto.startTime().isBefore(dto.endTime())) {
            throw new DomainRuleViolationException("Start time must be before end time");
        }
        if (isCreate && !dto.startDate().isAfter(LocalDate.now(clock))) {
            throw new DomainRuleViolationException("Start date must be in the future");
        }
        if (!dto.roleBalancingEnabled() && dto.roleBalanceThreshold() != null) {
            throw new DomainRuleViolationException(
                    "Role balance threshold requires role balancing to be enabled");
        }
    }

    private void applyDto(Course course, CreateCourseDto dto) {
        course.setTitle(dto.title());
        course.setDanceStyle(dto.danceStyle());
        course.setLevel(dto.level());
        course.setCourseType(dto.courseType());
        course.setDescription(dto.description());
        course.setStartDate(dto.startDate());
        course.setRecurrenceType(dto.recurrenceType());
        course.setDayOfWeek(dto.startDate().getDayOfWeek());
        course.setNumberOfSessions(dto.numberOfSessions());
        course.setEndDate(calculateEndDate(dto.startDate(), dto.recurrenceType(), dto.numberOfSessions()));
        course.setStartTime(dto.startTime());
        course.setEndTime(dto.endTime());
        course.setLocation(dto.location());
        course.setTeachers(dto.teachers());
        course.setMaxParticipants(dto.maxParticipants());
        course.setRoleBalancingEnabled(dto.roleBalancingEnabled());
        course.setRoleBalanceThreshold(dto.roleBalanceThreshold());
        course.setPriceModel(dto.priceModel());
        course.setPrice(dto.price());
        // status and publishDate from DTO are ignored — publishedAt is managed via publish endpoint
    }

    private LocalDate calculateEndDate(LocalDate startDate, RecurrenceType recurrenceType, int numberOfSessions) {
        int intervalWeeks = switch (recurrenceType) {
            case WEEKLY -> 1;
        };
        return startDate.plusWeeks((long) (numberOfSessions - 1) * intervalWeeks);
    }

    private CourseListDto toListDto(Course course, LocalDate today) {
        return new CourseListDto(
                course.getId(),
                course.getTitle(),
                course.getDanceStyle(),
                course.getLevel(),
                course.getDayOfWeek(),
                course.getStartTime(),
                course.getEndTime(),
                course.getNumberOfSessions(),
                course.getStartDate(),
                course.getEndDate(),
                course.getEnrolledStudents(),
                course.getMaxParticipants(),
                course.getPrice(),
                CourseStatusDerivation.deriveStatus(
                        course.getPublishedAt(), course.getStartDate(), course.getEndDate(), today),
                CourseStatusDerivation.deriveCompletedSessions(
                        course.getStartDate(), course.getDayOfWeek(), course.getNumberOfSessions(), today)
        );
    }

    private CourseDetailDto toDetailDto(Course course, LocalDate today) {
        return new CourseDetailDto(
                course.getId(),
                course.getTitle(),
                course.getDanceStyle(),
                course.getLevel(),
                course.getCourseType(),
                course.getDescription(),
                course.getStartDate(),
                course.getRecurrenceType(),
                course.getDayOfWeek(),
                course.getNumberOfSessions(),
                course.getEndDate(),
                course.getStartTime(),
                course.getEndTime(),
                course.getLocation(),
                course.getTeachers(),
                course.getMaxParticipants(),
                course.isRoleBalancingEnabled(),
                course.getRoleBalanceThreshold(),
                course.getPriceModel(),
                course.getPrice(),
                CourseStatusDerivation.deriveStatus(
                        course.getPublishedAt(), course.getStartDate(), course.getEndDate(), today),
                course.getPublishedAt(),
                course.getEnrolledStudents(),
                CourseStatusDerivation.deriveCompletedSessions(
                        course.getStartDate(), course.getDayOfWeek(), course.getNumberOfSessions(), today)
        );
    }
}
