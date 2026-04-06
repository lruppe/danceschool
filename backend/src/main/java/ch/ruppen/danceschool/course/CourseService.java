package ch.ruppen.danceschool.course;

import ch.ruppen.danceschool.school.School;
import ch.ruppen.danceschool.school.SchoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final SchoolService schoolService;

    public List<CourseListDto> getCoursesByMember(Long userId) {
        School school = schoolService.findSchoolByMember(userId);
        return courseRepository.findAllBySchoolId(school.getId()).stream()
                .map(this::toListDto)
                .toList();
    }

    public void seedCourse(Long userId, String title, DanceStyle danceStyle, CourseLevel level,
                           CourseType courseType, String description, java.time.LocalDate startDate,
                           RecurrenceType recurrenceType, java.time.DayOfWeek dayOfWeek,
                           int numberOfSessions, java.time.LocalTime startTime, java.time.LocalTime endTime,
                           String location, String teachers, int maxParticipants, int enrolledStudents,
                           PriceModel priceModel, java.math.BigDecimal price, CourseStatus status) {
        School school = schoolService.findSchoolByMember(userId);
        Course course = new Course();
        course.setSchool(school);
        course.setTitle(title);
        course.setDanceStyle(danceStyle);
        course.setLevel(level);
        course.setCourseType(courseType);
        course.setDescription(description);
        course.setStartDate(startDate);
        course.setRecurrenceType(recurrenceType);
        course.setDayOfWeek(dayOfWeek);
        course.setNumberOfSessions(numberOfSessions);
        course.setStartTime(startTime);
        course.setEndTime(endTime);
        course.setLocation(location);
        course.setTeachers(teachers);
        course.setMaxParticipants(maxParticipants);
        course.setEnrolledStudents(enrolledStudents);
        course.setPriceModel(priceModel);
        course.setPrice(price);
        course.setStatus(status);
        courseRepository.save(course);
    }

    public boolean hasCoursesForMember(Long userId) {
        School school = schoolService.findSchoolByMember(userId);
        return !courseRepository.findAllBySchoolId(school.getId()).isEmpty();
    }

    private CourseListDto toListDto(Course course) {
        return new CourseListDto(
                course.getId(),
                course.getTitle(),
                course.getDanceStyle(),
                course.getLevel(),
                course.getDayOfWeek(),
                course.getStartTime(),
                course.getEndTime(),
                course.getNumberOfSessions(),
                course.getEnrolledStudents(),
                course.getMaxParticipants(),
                course.getPrice(),
                course.getStatus()
        );
    }
}
