package ch.ruppen.danceschool.student;

import ch.ruppen.danceschool.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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
import java.util.Map;

@RestController
@RequestMapping("/api/students")
@PreAuthorize("@schoolAuthz.hasMembership()")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> create(@Valid @RequestBody CreateStudentDto dto,
                                    @AuthenticationPrincipal AuthenticatedUser principal) {
        Long id = studentService.createStudent(principal.userId(), dto);
        return Map.of("id", id);
    }

    @GetMapping
    public List<StudentListDto> list(@AuthenticationPrincipal AuthenticatedUser principal) {
        return studentService.listStudents(principal.userId());
    }

    @GetMapping("/{id}")
    public StudentDetailDto getDetail(@PathVariable Long id,
                                      @AuthenticationPrincipal AuthenticatedUser principal) {
        return studentService.getStudent(principal.userId(), id);
    }

    @PutMapping("/{id}/dance-levels")
    public UpdateDanceLevelsResultDto updateDanceLevels(@PathVariable Long id,
                                                        @Valid @RequestBody UpdateDanceLevelsDto dto,
                                                        @AuthenticationPrincipal AuthenticatedUser principal) {
        return studentService.updateDanceLevels(principal.userId(), id, dto);
    }
}
