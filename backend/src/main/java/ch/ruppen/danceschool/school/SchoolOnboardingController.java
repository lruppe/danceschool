package ch.ruppen.danceschool.school;

import ch.ruppen.danceschool.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Onboarding entrypoint that creates the caller's first {@code SchoolMember}. Callers reach
 * this with no membership yet, so the endpoint is intentionally not gated by
 * {@code @schoolAuthz.hasMembership()} and the controller is deliberately not
 * {@code @TenantScoped} — admin-authz rules only kick in once the school exists. Every
 * other school endpoint lives on {@code SchoolController} and requires membership.
 */
@RestController
@RequestMapping("/api/schools")
@RequiredArgsConstructor
public class SchoolOnboardingController {

    private final SchoolService schoolService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolDetailDto create(@Valid @RequestBody SchoolUpdateDto dto,
                                  @AuthenticationPrincipal AuthenticatedUser principal) {
        return schoolService.createSchool(dto, principal.userId());
    }
}
