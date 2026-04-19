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

        SchoolMember enrolledBy = schoolMemberService.findByUserIdAndSchoolId(userId, school.getId())
                .orElse(null);

        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setDanceRole(dto.danceRole());
        enrollment.setEnrolledAt(Instant.now(clock));
        enrollment.setEnrolledBy(enrolledBy);

        EnrollmentStatus bookingStatus = resolveBookingStatus(course, student);
        // PENDING_APPROVAL applicants queue for owner review — they don't reserve seats at enroll time,
        // so capacity / role-balance only gate the committed (direct-pay) path. Approval re-checks capacity.
        WaitlistDecision waitlist = (bookingStatus == EnrollmentStatus.PENDING_PAYMENT)
                ? resolveWaitlist(course, dto.danceRole())
                : null;
        if (waitlist != null) {
            enrollment.setStatus(EnrollmentStatus.WAITLISTED);
            enrollment.setWaitlistReason(waitlist.reason());
            enrollment.setWaitlistPosition(waitlist.position());
        } else {
            enrollment.setStatus(bookingStatus);
        }

        enrollmentRepository.save(enrollment);
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

        enrollment.setApprovedAt(Instant.now(clock));

        Course course = enrollment.getCourse();
        upsertStudentDanceLevel(enrollment.getStudent(), course.getDanceStyle(), course.getLevel());

        // If the course filled up between application and approval, route to the waitlist.
        long committedCount = enrollmentRepository.countByCourseIdAndStatusIn(
                course.getId(), EnrollmentStatus.SEAT_HOLDING_STATI);
        if (committedCount >= course.getMaxParticipants()) {
            // Compute position BEFORE flipping to WAITLISTED so Hibernate's auto-flush
            // doesn't count this enrollment against itself.
            int position = nextPosition(course.getId(), enrollment.getDanceRole());
            enrollment.setStatus(EnrollmentStatus.WAITLISTED);
            enrollment.setWaitlistReason(WaitlistReason.CAPACITY);
            enrollment.setWaitlistPosition(position);
        } else {
            enrollment.setStatus(EnrollmentStatus.PENDING_PAYMENT);
        }

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

    private WaitlistDecision resolveWaitlist(Course course, DanceRole role) {
        long committed = enrollmentRepository.countByCourseIdAndStatusIn(course.getId(), EnrollmentStatus.SEAT_HOLDING_STATI);
        if (committed >= course.getMaxParticipants()) {
            return new WaitlistDecision(WaitlistReason.CAPACITY, nextPosition(course.getId(), role));
        }

        if (course.getCourseType() == CourseType.PARTNER
                && course.getRoleBalanceThreshold() != null
                && role != null) {
            DanceRole other = (role == DanceRole.LEAD) ? DanceRole.FOLLOW : DanceRole.LEAD;
            long myCount = enrollmentRepository.countByCourseIdAndDanceRoleAndStatusIn(
                    course.getId(), role, EnrollmentStatus.SEAT_HOLDING_STATI);
            long otherCount = enrollmentRepository.countByCourseIdAndDanceRoleAndStatusIn(
                    course.getId(), other, EnrollmentStatus.SEAT_HOLDING_STATI);
            if ((myCount + 1) > otherCount + course.getRoleBalanceThreshold()) {
                return new WaitlistDecision(WaitlistReason.ROLE_IMBALANCE, nextPosition(course.getId(), role));
            }
        }

        return null;
    }

    private int nextPosition(Long courseId, DanceRole role) {
        long existing = (role == null)
                ? enrollmentRepository.countByCourseIdAndStatus(courseId, EnrollmentStatus.WAITLISTED)
                : enrollmentRepository.countByCourseIdAndStatusAndDanceRole(courseId, EnrollmentStatus.WAITLISTED, role);
        return (int) existing + 1;
    }

    private record WaitlistDecision(WaitlistReason reason, int position) {
    }

    private EnrollmentStatus resolveBookingStatus(Course course, Student student) {
        // BEGINNER and STARTER courses always skip approval
        if (course.getLevel().ordinal() <= CourseLevel.BEGINNER.ordinal()) {
            return EnrollmentStatus.PENDING_PAYMENT;
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
