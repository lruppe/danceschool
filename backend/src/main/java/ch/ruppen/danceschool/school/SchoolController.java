package ch.ruppen.danceschool.school;

import ch.ruppen.danceschool.shared.security.AuthenticatedUser;
import ch.ruppen.danceschool.shared.security.TenantScoped;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schools")
@PreAuthorize("@schoolAuthz.hasMembership()")
@TenantScoped
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolService schoolService;

    @GetMapping("/me")
    public SchoolDetailDto me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return schoolService.getByMemberUserId(principal.userId());
    }

    @PutMapping("/me")
    public SchoolDetailDto updateMe(@Valid @RequestBody SchoolUpdateDto dto,
                                    @AuthenticationPrincipal AuthenticatedUser principal) {
        return schoolService.updateSchool(principal.userId(), dto);
    }
}
