package ch.ruppen.danceschool.student;

public record StudentListDto(
        Long id,
        String name,
        String email,
        String phoneNumber,
        long activeCoursesCount
) {
}
