package ch.ruppen.danceschool.student;

import ch.ruppen.danceschool.course.DanceStyle;
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
    public StudentDetailDto updateDanceLevels(Long userId, Long studentId, UpdateDanceLevelsDto dto) {
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

        return toDetailDto(student);
    }

    @Transactional(readOnly = true)
    public Student findStudentByIdAndSchool(Long studentId, School school) {
        return studentRepository.findByIdAndSchoolId(studentId, school.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", studentId));
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
