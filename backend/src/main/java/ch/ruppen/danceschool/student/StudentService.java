package ch.ruppen.danceschool.student;

import ch.ruppen.danceschool.course.Course;
import ch.ruppen.danceschool.course.CourseLevel;
import ch.ruppen.danceschool.course.DanceStyle;
import ch.ruppen.danceschool.enrollment.Enrollment;
import ch.ruppen.danceschool.enrollment.EnrollmentRepository;
import ch.ruppen.danceschool.enrollment.EnrollmentResponseDto;
import ch.ruppen.danceschool.enrollment.EnrollmentService;
import ch.ruppen.danceschool.enrollment.EnrollmentStatus;
import ch.ruppen.danceschool.school.School;
import ch.ruppen.danceschool.school.SchoolService;
import ch.ruppen.danceschool.shared.error.ResourceNotFoundException;
import ch.ruppen.danceschool.shared.logging.BusinessOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final SchoolService schoolService;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentService enrollmentService;
    private final Clock clock;

    @Transactional
    @BusinessOperation(event = "StudentCreated")
    public Long createStudent(Long userId, CreateStudentDto dto) {
        School school = schoolService.findSchoolByMember(userId);

        Student student = new Student();
        student.setSchool(school);
        student.setName(dto.name());
        student.setEmail(dto.email());
        student.setPhoneNumber(dto.phoneNumber());

        if (dto.danceLevels() != null) {
            for (CreateStudentDto.DanceLevelEntry entry : dto.danceLevels()) {
                StudentDanceLevel level = new StudentDanceLevel();
                level.setStudent(student);
                level.setDanceStyle(entry.danceStyle());
                level.setLevel(entry.level());
                student.getDanceLevels().add(level);
            }
        }

        studentRepository.save(student);
        return student.getId();
    }

    @Transactional(readOnly = true)
    public List<StudentListDto> listStudents(Long userId) {
        School school = schoolService.findSchoolByMember(userId);
        return studentRepository.findListBySchoolId(
                school.getId(),
                EnrollmentStatus.SEAT_HOLDING_STATI,
                LocalDate.now(clock));
    }

    @Transactional(readOnly = true)
    public StudentDetailDto getStudent(Long userId, Long studentId) {
        School school = schoolService.findSchoolByMember(userId);
        Student student = studentRepository.findByIdAndSchoolId(studentId, school.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
        return toDetailDto(student);
    }

    @Transactional
    @BusinessOperation(event = "StudentDanceLevelsUpdated")
    public UpdateDanceLevelsResultDto updateDanceLevels(Long userId, Long studentId, UpdateDanceLevelsDto dto) {
        School school = schoolService.findSchoolByMember(userId);
        Student student = studentRepository.findByIdAndSchoolId(studentId, school.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));

        // Diff-in-place so we don't delete-then-insert unchanged styles — with IDENTITY ids and
        // the (student_id, dance_style) unique constraint, clear()+add() inserts before orphan
        // removal flushes and trips the constraint.
        Map<DanceStyle, StudentDanceLevel> existing = student.getDanceLevels().stream()
                .collect(Collectors.toMap(StudentDanceLevel::getDanceStyle, Function.identity()));
        Set<DanceStyle> incoming = dto.danceLevels().stream()
                .map(UpdateDanceLevelsDto.DanceLevelEntry::danceStyle)
                .collect(Collectors.toSet());

        // Only a level that's added or raised can unblock a pending approval; tracking these up
        // front lets us restrict the re-evaluation query to the enrollments actually affected.
        Set<DanceStyle> raisedStyles = EnumSet.noneOf(DanceStyle.class);
        for (UpdateDanceLevelsDto.DanceLevelEntry entry : dto.danceLevels()) {
            StudentDanceLevel current = existing.get(entry.danceStyle());
            if (current == null || entry.level().ordinal() > current.getLevel().ordinal()) {
                raisedStyles.add(entry.danceStyle());
            }
        }

        student.getDanceLevels().removeIf(dl -> !incoming.contains(dl.getDanceStyle()));

        for (UpdateDanceLevelsDto.DanceLevelEntry entry : dto.danceLevels()) {
            StudentDanceLevel current = existing.get(entry.danceStyle());
            if (current != null) {
                current.setLevel(entry.level());
            } else {
                StudentDanceLevel level = new StudentDanceLevel();
                level.setStudent(student);
                level.setDanceStyle(entry.danceStyle());
                level.setLevel(entry.level());
                student.getDanceLevels().add(level);
            }
        }

        int autoConfirmedCount = autoConfirmPendingForRaisedStyles(userId, student, raisedStyles);

        return new UpdateDanceLevelsResultDto(toDetailDto(student), autoConfirmedCount);
    }

    private int autoConfirmPendingForRaisedStyles(Long userId, Student student, Set<DanceStyle> raisedStyles) {
        if (raisedStyles.isEmpty()) {
            return 0;
        }
        List<Enrollment> candidates = enrollmentRepository.findByStudentIdAndStatusAndCourseDanceStyleIn(
                student.getId(), EnrollmentStatus.PENDING_APPROVAL, raisedStyles);

        int count = 0;
        for (Enrollment enrollment : candidates) {
            Course course = enrollment.getCourse();
            CourseLevel newLevel = findLevel(student, course.getDanceStyle());
            // Skip enrollments whose course still outranks the new level — those stay pending.
            // Also guards approveEnrollment's upsertStudentDanceLevel from silently promoting
            // the student past what the owner explicitly set.
            if (newLevel == null || newLevel.ordinal() < course.getLevel().ordinal()) {
                continue;
            }
            EnrollmentResponseDto result = enrollmentService.approveEnrollment(userId, enrollment.getId());
            // WAITLISTED means the re-evaluation promoted the enrollment past approval but
            // couldn't seat it (capacity/role-balance); not a user-visible "auto-confirmed".
            if (EnrollmentStatus.SEAT_HOLDING_STATI.contains(result.status())) {
                count++;
            }
        }
        return count;
    }

    private CourseLevel findLevel(Student student, DanceStyle style) {
        return student.getDanceLevels().stream()
                .filter(dl -> dl.getDanceStyle() == style)
                .map(StudentDanceLevel::getLevel)
                .findFirst()
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean hasStudentsForSchool(Long userId) {
        School school = schoolService.findSchoolByMember(userId);
        return studentRepository.existsBySchoolId(school.getId());
    }

    /**
     * Seeds a student for dev/test data directly via the school entity.
     */
    @Transactional
    public Student seedStudent(School school, String name, String email, String phoneNumber,
                               List<CreateStudentDto.DanceLevelEntry> danceLevels) {
        Student student = new Student();
        student.setSchool(school);
        student.setName(name);
        student.setEmail(email);
        student.setPhoneNumber(phoneNumber);

        if (danceLevels != null) {
            for (CreateStudentDto.DanceLevelEntry entry : danceLevels) {
                StudentDanceLevel level = new StudentDanceLevel();
                level.setStudent(student);
                level.setDanceStyle(entry.danceStyle());
                level.setLevel(entry.level());
                student.getDanceLevels().add(level);
            }
        }

        return studentRepository.save(student);
    }

    private StudentDetailDto toDetailDto(Student student) {
        return new StudentDetailDto(
                student.getId(),
                student.getName(),
                student.getEmail(),
                student.getPhoneNumber(),
                student.getDanceLevels().stream()
                        .map(dl -> new StudentDetailDto.DanceLevelDto(dl.getDanceStyle(), dl.getLevel()))
                        .toList()
        );
    }
}
