package ch.ruppen.danceschool.course;

import ch.ruppen.danceschool.shared.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping("/me")
    public List<CourseListDto> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        return courseService.getCoursesByMember(principal.userId());
    }
}
