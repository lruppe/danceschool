package ch.ruppen.danceschool.schoolmember;

public record MembershipDto(
        Long schoolId,
        String schoolName,
        MemberRole role
) {
}
