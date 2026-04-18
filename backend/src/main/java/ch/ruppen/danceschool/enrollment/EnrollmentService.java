package ch.ruppen.danceschool.enrollment;

import ch.ruppen.danceschool.course.Course;
import ch.ruppen.danceschool.course.CourseLevel;
import ch.ruppen.danceschool.course.CourseService;
import ch.ruppen.danceschool.course.CourseType;
import ch.ruppen.danceschool.course.DanceStyle;
import ch.ruppen.danceschool.school.School;
import ch.ruppen.danceschool.school.SchoolService;
import ch.ruppen.danceschool.schoolmember.SchoolMember;
import ch.ruppen.danceschool.schoolmember.SchoolMemberService;
import ch.ruppen.danceschool.shared.error.DomainRuleViolationException;
import ch.ruppen.danceschool.shared.error.ResourceNotFoundException;
import ch.ruppen.danceschool.shared.logging.BusinessOperation;
import ch.ruppen.danceschool.student.Student;
import ch.ruppen.danceschool.student.StudentDanceLevel;
import ch.ruppen.danceschool.student.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final SchoolService schoolService;
    private final CourseService courseService;
    private final StudentService studentService;
    private final SchoolMemberService schoolMemberService;
    private final Clock clock;

    @Transactional
    @BusinessOperation(event = "StudentEnrolled")
    public EnrollmentResponseDto enrollStudent(Long userId, Long courseId, EnrollStudentDto dto) {
        School school = schoolService.findSchoolByMember(userId);
        Course course = courseService.findCourseByIdAndSchool(courseId, school);
        Student student = studentService.findStudentByIdAndSchool(dto.studentId(), school);

        validateDanceRole(course, dto.danceRole());
        validateNoDuplicate(student.getId(), course.getId());
        validateCapacity(course);

        SchoolMember enrolledBy = schoolMemberService.findByUserIdAndSchoolId(userId, school.getId())
                .orElse(null);

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setDanceRole(dto.danceRole());
        enrollment.setStatus(resolveBookingStatus(course, student));
        enrollment.setEnrolledAt(Instant.now(clock));
        enrollment.setEnrolledBy(enrolledBy);

        enrollmentRepository.save(enrollment);
        course.setEnrolledStudents(course.getEnrolledStudents() + 1);
        return new EnrollmentResponseDto(enrollment.getId(), enrollment.getStatus());
    }

    @Transactional
    @BusinessOperation(event = "EnrollmentPaymentConfirmed")
    public EnrollmentResponseDto confirmPayment(Long userId, Long enrollmentId) {
        School school = schoolService.findSchoolByMember(userId);
        Enrollment enrollment = enrollmentRepository.findByIdAndCourseSchoolId(enrollmentId, school.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", enrollmentId));

        if (enrollment.getStatus() != EnrollmentStatus.PENDING_PAYMENT) {
            throw new DomainRuleViolationException("Enrollment is not pending payment");
        }

        enrollment.setStatus(EnrollmentStatus.CONFIRMED);
        enrollment.setPaidAt(Instant.now(clock));
        return new EnrollmentResponseDto(enrollment.getId(), enrollment.getStatus());
    }

    @Transactional
    @BusinessOperation(event = "EnrollmentApproved")
    public EnrollmentResponseDto approveEnrollment(Long userId, Long enrollmentId) {
        School school = schoolService.findSchoolByMember(userId);
        Enrollment enrollment = enrollmentRepository.findByIdAndCourseSchoolId(enrollmentId, school.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", enrollmentId));

        if (enrollment.getStatus() != EnrollmentStatus.PENDING_APPROVAL) {
            throw new DomainRuleViolationException("Enrollment is not pending approval");
        }

        enrollment.setStatus(EnrollmentStatus.PENDING_PAYMENT);
        enrollment.setApprovedAt(Instant.now(clock));

        Course course = enrollment.getCourse();
        upsertStudentDanceLevel(enrollment.getStudent(), course.getDanceStyle(), course.getLevel());

        return new EnrollmentResponseDto(enrollment.getId(), enrollment.getStatus());
    }

    @Transactional
    @BusinessOperation(event = "EnrollmentRejected")
    public EnrollmentResponseDto rejectEnrollment(Long userId, Long enrollmentId) {
        School school = schoolService.findSchoolByMember(userId);
        Enrollment enrollment = enrollmentRepository.findByIdAndCourseSchoolId(enrollmentId, school.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment", enrollmentId));

        if (enrollment.getStatus() != EnrollmentStatus.PENDING_APPROVAL) {
            throw new DomainRuleViolationException("Enrollment is not pending approval");
        }

        enrollment.setStatus(EnrollmentStatus.REJECTED);
        return new EnrollmentResponseDto(enrollment.getId(), enrollment.getStatus());
    }

    @Transactional(readOnly = true)
    public List<EnrollmentListDto> getEnrollments(Long userId, Long courseId) {
        School school = schoolService.findSchoolByMember(userId);
        courseService.findCourseByIdAndSchool(courseId, school);

        return enrollmentRepository.findAllByCourseId(courseId).stream()
                .map(this::toListDto)
                .toList();
    }

    @Transactional
    public void seedEnrollment(Course course, Student student, DanceRole danceRole,
                               EnrollmentStatus status, Instant enrolledAt, Instant paidAt) {
        Enrollment enrollment = new Enrollment();
        enrollment.setCourse(course);
        enrollment.setStudent(student);
        enrollment.setDanceRole(danceRole);
        enrollment.setStatus(status);
        enrollment.setEnrolledAt(enrolledAt);
        enrollment.setPaidAt(paidAt);
        enrollmentRepository.save(enrollment);
    }

    private void validateDanceRole(Course course, DanceRole danceRole) {
        if (course.getCourseType() == CourseType.PARTNER && danceRole == null) {
            throw new DomainRuleViolationException("Dance role is required for partner courses");
        }
        if (course.getCourseType() == CourseType.SOLO && danceRole != null) {
            throw new DomainRuleViolationException("Dance role must not be set for solo courses");
        }
    }

    private void validateNoDuplicate(Long studentId, Long courseId) {
        if (enrollmentRepository.existsByStudentIdAndCourseIdAndStatusNot(
                studentId, courseId, EnrollmentStatus.REJECTED)) {
            throw new DomainRuleViolationException("Student is already enrolled in this course");
        }
    }

    private void validateCapacity(Course course) {
        long activeCount = enrollmentRepository.countByCourseIdAndStatusIn(
                course.getId(),
                List.of(EnrollmentStatus.PENDING_APPROVAL,
                        EnrollmentStatus.PENDING_PAYMENT,
                        EnrollmentStatus.CONFIRMED));
        if (activeCount >= course.getMaxParticipants()) {
            throw new DomainRuleViolationException("Course is at capacity");
        }
    }

    private EnrollmentStatus resolveBookingStatus(Course course, Student student) {
        // BEGINNER and STARTER courses always skip approval
        if (course.getLevel().ordinal() <= CourseLevel.BEGINNER.ordinal()) {
            return EnrollmentStatus.PENDING_PAYMENT;
        }

        if (course.isRequiresApproval()) {
            return EnrollmentStatus.PENDING_APPROVAL;
        }

        CourseLevel studentLevel = findStudentLevel(student, course.getDanceStyle());
        if (studentLevel == null || studentLevel.ordinal() < course.getLevel().ordinal()) {
            return EnrollmentStatus.PENDING_APPROVAL;
        }

        return EnrollmentStatus.PENDING_PAYMENT;
    }

    private CourseLevel findStudentLevel(Student student, DanceStyle style) {
        return student.getDanceLevels().stream()
                .filter(dl -> dl.getDanceStyle() == style)
                .map(StudentDanceLevel::getLevel)
                .findFirst()
                .orElse(null);
    }

    private void upsertStudentDanceLevel(Student student, DanceStyle style, CourseLevel level) {
        StudentDanceLevel existing = student.getDanceLevels().stream()
                .filter(dl -> dl.getDanceStyle() == style)
                .findFirst()
                .orElse(null);

        if (existing == null) {
            StudentDanceLevel dl = new StudentDanceLevel();
            dl.setStudent(student);
            dl.setDanceStyle(style);
            dl.setLevel(level);
            student.getDanceLevels().add(dl);
        } else if (existing.getLevel().ordinal() < level.ordinal()) {
            existing.setLevel(level);
        }
    }

    private EnrollmentListDto toListDto(Enrollment enrollment) {
        Student student = enrollment.getStudent();
        Course course = enrollment.getCourse();
        return new EnrollmentListDto(
                enrollment.getId(),
                student.getName(),
                student.getEmail(),
                student.getPhoneNumber(),
                enrollment.getDanceRole(),
                enrollment.getStatus(),
                enrollment.getEnrolledAt(),
                enrollment.getApprovedAt(),
                enrollment.getPaidAt(),
                enrollment.getWaitlistPosition(),
                enrollment.getWaitlistReason(),
                findStudentLevel(student, course.getDanceStyle())
        );
    }
}
