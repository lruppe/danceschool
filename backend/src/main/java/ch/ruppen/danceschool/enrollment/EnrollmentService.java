package ch.ruppen.danceschool.enrollment;

import ch.ruppen.danceschool.course.Course;
import ch.ruppen.danceschool.course.CourseLevel;
import ch.ruppen.danceschool.course.CourseRepository;
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
import ch.ruppen.danceschool.student.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    // Dedicated logger shared with BusinessLoggingAspect — auto-promote emits directly
    // (self-invocation bypasses the aspect) but must land on the same logger so downstream
    // filters treat both paths identically.
    private static final Logger businessLog = LoggerFactory.getLogger("business");

    private final EnrollmentRepository enrollmentRepository;
    private final SchoolService schoolService;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final SchoolMemberService schoolMemberService;
    private final Clock clock;

    @Transactional
    @BusinessOperation(event = "StudentEnrolled")
    public EnrollmentResponseDto enrollStudent(Long userId, Long courseId, EnrollStudentDto dto) {
        School school = schoolService.findSchoolByMember(userId);
        Course course = loadCourseInSchool(courseId, school);
        Student student = studentRepository.findByIdAndSchoolId(dto.studentId(), school.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Student", dto.studentId()));

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

        applyBookingDecision(enrollment);

        enrollmentRepository.save(enrollment);

        if (enrollment.getStatus() == EnrollmentStatus.PENDING_PAYMENT) {
            autoPromoteWaitlist(course);
        }
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

        // Order matters: upsert first so the level gate now passes, then apply the same
        // capacity + role-balance checks the direct-pay path runs.
        applyBookingDecision(enrollment);

        if (enrollment.getStatus() == EnrollmentStatus.PENDING_PAYMENT) {
            autoPromoteWaitlist(course);
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
    public Map<Long, RoleCounts> countSeatHoldersByRoleGroupedByCourse(List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, RoleCounts> result = new HashMap<>();
        for (Object[] row : enrollmentRepository.countByRoleGroupedByCourse(courseIds, EnrollmentStatus.SEAT_HOLDING_STATI)) {
            Long courseId = (Long) row[0];
            DanceRole role = (DanceRole) row[1];
            int count = ((Number) row[2]).intValue();
            RoleCounts existing = result.getOrDefault(courseId, RoleCounts.EMPTY);
            result.put(courseId, existing.plus(role, count));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public Map<Long, Integer> countSeatHoldersGroupedByCourse(List<Long> courseIds) {
        if (courseIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Integer> result = new HashMap<>();
        for (Object[] row : enrollmentRepository.countGroupedByCourse(courseIds, EnrollmentStatus.SEAT_HOLDING_STATI)) {
            result.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return result;
    }

    @Transactional(readOnly = true)
    public int countSeatHoldersByCourse(Long courseId) {
        return (int) enrollmentRepository.countByCourseIdAndStatusIn(
                courseId, EnrollmentStatus.SEAT_HOLDING_STATI);
    }

    @Transactional(readOnly = true)
    public int countNonTerminalByCourse(Long courseId) {
        return (int) enrollmentRepository.countByCourseIdAndStatusIn(
                courseId, EnrollmentStatus.NON_TERMINAL_STATI);
    }

    private Course loadCourseInSchool(Long courseId, School school) {
        return courseRepository.findByIdAndSchoolId(courseId, school.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", courseId));
    }

    @Transactional(readOnly = true)
    public List<EnrollmentListDto> getEnrollments(Long userId, Long courseId) {
        School school = schoolService.findSchoolByMember(userId);
        loadCourseInSchool(courseId, school);

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

    private void applyBookingDecision(Enrollment enrollment) {
        Course course = enrollment.getCourse();
        EnrollmentStatus bookingStatus = resolveBookingStatus(course, enrollment.getStudent());
        // PENDING_APPROVAL doesn't reserve a seat, so skip capacity/role-balance until approval
        // promotes it to a committed status.
        WaitlistDecision waitlist = (bookingStatus == EnrollmentStatus.PENDING_PAYMENT)
                ? resolveWaitlist(course, enrollment.getDanceRole())
                : null;
        if (waitlist != null) {
            enrollment.setStatus(EnrollmentStatus.WAITLISTED);
            enrollment.setWaitlistReason(waitlist.reason());
            enrollment.setWaitlistPosition(waitlist.position());
        } else {
            enrollment.setStatus(bookingStatus);
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

    /**
     * Re-evaluates the waitlist against current capacity and role-balance rules and promotes
     * any entries that no longer violate them. Called after:
     * <ul>
     *   <li>a seat-holding enrollment lands (may free up role-imbalance waitlisted entries of
     *       the opposite role)</li>
     *   <li>a course's {@code maxParticipants} is raised (promotes capacity-waitlisted entries
     *       in waitlist order until capacity is reached)</li>
     *   <li>a course's {@code roleBalanceThreshold} changes (promotes role-imbalance
     *       waitlisted entries that now fit the looser rule)</li>
     * </ul>
     * Short-circuits when the course is already at or over capacity.
     */
    public void autoPromoteWaitlist(Course course) {
        long committed = enrollmentRepository.countByCourseIdAndStatusIn(
                course.getId(), EnrollmentStatus.SEAT_HOLDING_STATI);
        if (committed >= course.getMaxParticipants()) {
            return;
        }

        List<Enrollment> candidates = enrollmentRepository
                .findByCourseIdAndStatusOrderByWaitlistPositionAsc(
                        course.getId(), EnrollmentStatus.WAITLISTED);

        boolean anyPromoted = false;
        for (Enrollment candidate : candidates) {
            if (resolveWaitlist(course, candidate.getDanceRole()) == null) {
                candidate.setStatus(EnrollmentStatus.PENDING_PAYMENT);
                candidate.setWaitlistReason(null);
                candidate.setWaitlistPosition(null);
                // Matches the BusinessLoggingAspect output format; emitted directly because
                // self-invocation bypasses the aspect.
                businessLog.info("event=EnrollmentAutoPromoted enrollmentId={} status={}",
                        candidate.getId(), EnrollmentStatus.PENDING_PAYMENT);
                committed++;
                anyPromoted = true;
                if (committed >= course.getMaxParticipants()) {
                    break;
                }
            }
        }
        if (anyPromoted) {
            renumberWaitlistPositions(course);
        }
    }

    private void renumberWaitlistPositions(Course course) {
        List<Enrollment> remaining = enrollmentRepository
                .findByCourseIdAndStatusOrderByWaitlistPositionAsc(
                        course.getId(), EnrollmentStatus.WAITLISTED);

        Map<DanceRole, List<Enrollment>> byRole = new HashMap<>();
        for (Enrollment e : remaining) {
            byRole.computeIfAbsent(e.getDanceRole(), k -> new ArrayList<>()).add(e);
        }
        for (List<Enrollment> entries : byRole.values()) {
            int pos = 1;
            for (Enrollment e : entries) {
                e.setWaitlistPosition(pos++);
            }
        }
    }

    private int nextPosition(Long courseId, DanceRole role) {
        long existing = (role == null)
                ? enrollmentRepository.countWaitlistedByCourse(courseId)
                : enrollmentRepository.countWaitlistedByCourseAndRole(courseId, role);
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
