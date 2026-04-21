package ch.ruppen.danceschool.student;

public record UpdateDanceLevelsResultDto(
        StudentDetailDto student,
        int autoConfirmedCount
) {}
