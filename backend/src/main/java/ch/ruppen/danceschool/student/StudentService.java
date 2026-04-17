package ch.ruppen.danceschool.student;

import ch.ruppen.danceschool.school.School;
import ch.ruppen.danceschool.school.SchoolService;
import ch.ruppen.danceschool.shared.error.ResourceNotFoundException;
import ch.ruppen.danceschool.shared.logging.BusinessOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final SchoolService schoolService;

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

        student.getDanceLevels().clear();
        for (UpdateDanceLevelsDto.DanceLevelEntry entry : dto.danceLevels()) {
            StudentDanceLevel level = new StudentDanceLevel();
            level.setStudent(student);
            level.setDanceStyle(entry.danceStyle());
            level.setLevel(entry.level());
            student.getDanceLevels().add(level);
        }

        return toDetailDto(student);
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
