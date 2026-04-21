package ch.ruppen.danceschool.payment;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentDto(
        Long enrollmentId,
        String studentName,
        String studentEmail,
        String courseTitle,
        BigDecimal amount,
        PaymentStatus status,
        Instant billingDate
) {
}
