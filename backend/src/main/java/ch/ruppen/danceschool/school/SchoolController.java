package ch.ruppen.danceschool.school;

import ch.ruppen.danceschool.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schools")
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolService schoolService;

    /**
     * Onboarding entrypoint that creates the caller's first {@code SchoolMember}, so callers
     * reach this with no membership yet — intentionally not guarded by
     * {@code @schoolAuthz.hasMembership()}. All other admin endpoints require membership.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolDetailDto create(@Valid @RequestBody SchoolUpdateDto dto, @AuthenticationPrincipal AuthenticatedUser principal) {
        return schoolService.createSchool(dto, principal.userId());
    }

    @GetMapping("/me")
    @PreAuthorize("@schoolAuthz.hasMembership()")
    public SchoolDetailDto me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return schoolService.getByMemberUserId(principal.userId());
    }

    @PutMapping("/me")
    @PreAuthorize("@schoolAuthz.hasMembership()")
    public SchoolDetailDto updateMe(@Valid @RequestBody SchoolUpdateDto dto,
                                    @AuthenticationPrincipal AuthenticatedUser principal) {
        return schoolService.updateSchool(principal.userId(), dto);
    }
}
