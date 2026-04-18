package ch.ruppen.danceschool.enrollment;

import ch.ruppen.danceschool.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/courses/{courseId}/enrollments")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentResponseDto enroll(@PathVariable Long courseId,
                                        @Valid @RequestBody EnrollStudentDto dto,
                                        @AuthenticationPrincipal AuthenticatedUser principal) {
        return enrollmentService.enrollStudent(principal.userId(), courseId, dto);
    }

    @GetMapping("/courses/{courseId}/enrollments")
    public List<EnrollmentListDto> list(@PathVariable Long courseId,
                                        @AuthenticationPrincipal AuthenticatedUser principal) {
        return enrollmentService.getEnrollments(principal.userId(), courseId);
    }

    @PutMapping("/enrollments/{enrollmentId}/mark-paid")
    public EnrollmentResponseDto markPaid(@PathVariable Long enrollmentId,
                                          @AuthenticationPrincipal AuthenticatedUser principal) {
        return enrollmentService.confirmPayment(principal.userId(), enrollmentId);
    }

    @PutMapping("/enrollments/{enrollmentId}/approve")
    public EnrollmentResponseDto approve(@PathVariable Long enrollmentId,
                                         @AuthenticationPrincipal AuthenticatedUser principal) {
        return enrollmentService.approveEnrollment(principal.userId(), enrollmentId);
    }

    @PutMapping("/enrollments/{enrollmentId}/reject")
    public EnrollmentResponseDto reject(@PathVariable Long enrollmentId,
                                        @AuthenticationPrincipal AuthenticatedUser principal) {
        return enrollmentService.rejectEnrollment(principal.userId(), enrollmentId);
    }
}
