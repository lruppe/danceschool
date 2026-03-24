package ch.ruppen.danceschool.user;

import ch.ruppen.danceschool.schoolmember.MembershipDto;

import java.util.List;

public record UserDto(
        Long id,
        String email,
        String name,
        String avatarUrl,
        List<MembershipDto> memberships
) {
}
