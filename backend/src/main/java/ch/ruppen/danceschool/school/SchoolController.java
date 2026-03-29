package ch.ruppen.danceschool.school;

import ch.ruppen.danceschool.shared.error.ResourceNotFoundException;
import ch.ruppen.danceschool.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    private final CreateSchoolUseCase createSchoolUseCase;
    private final SchoolService schoolService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolDto create(@Valid @RequestBody SchoolDto dto, @AuthenticationPrincipal AuthenticatedUser principal) {
        return createSchoolUseCase.execute(dto, principal.userId());
    }

    @GetMapping("/me")
    public SchoolDetailDto me(@AuthenticationPrincipal AuthenticatedUser principal) {
        School school = schoolService.findByOwnerUserId(principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException("School", principal.userId()));
        return schoolService.toDetailDto(school);
    }

    @PutMapping("/me")
    public SchoolDetailDto updateMe(@Valid @RequestBody SchoolUpdateDto dto,
                                    @AuthenticationPrincipal AuthenticatedUser principal) {
        School school = schoolService.findByOwnerUserId(principal.userId())
                .orElseThrow(() -> new ResourceNotFoundException("School", principal.userId()));
        School updated = schoolService.updateSchool(school, dto);
        return schoolService.toDetailDto(updated);
    }
}
