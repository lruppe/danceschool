package ch.ruppen.danceschool.payment;

import ch.ruppen.danceschool.shared.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping
    public List<PaymentDto> list(@AuthenticationPrincipal AuthenticatedUser principal) {
        return paymentService.listForCurrentUser(principal.userId());
    }
}
