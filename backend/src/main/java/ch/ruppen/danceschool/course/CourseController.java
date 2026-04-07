package ch.ruppen.danceschool.course;

import ch.ruppen.danceschool.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping("/me")
    public List<CourseListDto> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return courseService.getCoursesByMember(principal.userId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> create(@Valid @RequestBody CreateCourseDto dto,
                                    @AuthenticationPrincipal AuthenticatedUser principal) {
        Long id = courseService.createCourse(principal.userId(), dto);
        return Map.of("id", id);
    }

    @GetMapping("/{id}")
    public CourseDetailDto getDetail(@PathVariable Long id,
                                    @AuthenticationPrincipal AuthenticatedUser principal) {
        return courseService.getCourseDetail(principal.userId(), id);
    }

    @PutMapping("/{id}")
    public CourseDetailDto update(@PathVariable Long id,
                                 @Valid @RequestBody CreateCourseDto dto,
                                 @AuthenticationPrincipal AuthenticatedUser principal) {
        return courseService.updateCourse(principal.userId(), id, dto);
    }
}
