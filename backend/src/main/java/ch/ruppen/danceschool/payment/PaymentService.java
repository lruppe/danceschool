package ch.ruppen.danceschool.payment;

import ch.ruppen.danceschool.enrollment.EnrollmentRepository;
import ch.ruppen.danceschool.school.School;
import ch.ruppen.danceschool.school.SchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final EnrollmentRepository enrollmentRepository;
    private final SchoolService schoolService;

    @Transactional(readOnly = true)
    public List<PaymentDto> listForCurrentUser(Long userId) {
        School school = schoolService.findSchoolByMember(userId);
        return enrollmentRepository.findPaymentsBySchoolId(school.getId());
    }
}
