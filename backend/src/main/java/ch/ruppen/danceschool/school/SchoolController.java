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

@RestController
@RequestMapping("/api/schools")
@RequiredArgsConstructor
public class SchoolController {

    private final CreateSchoolUseCase createSchoolUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SchoolDto create(@Valid @RequestBody SchoolDto dto, @AuthenticationPrincipal AuthenticatedUser principal) {
        return createSchoolUseCase.execute(dto, principal.userId());
    }
}
