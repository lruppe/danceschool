package ch.ruppen.danceschool.payment;

import ch.ruppen.danceschool.shared.security.AuthenticatedUser;
import ch.ruppen.danceschool.shared.security.TenantScoped;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@PreAuthorize("@schoolAuthz.hasMembership()")
@TenantScoped
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/me")
    public List<PaymentDto> listMine(@AuthenticationPrincipal AuthenticatedUser principal) {
        return paymentService.listForCurrentUser(principal.userId());
    }
}
