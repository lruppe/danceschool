package ch.ruppen.danceschool.shared.security;

public record AuthenticatedUser(Long userId, String email) {
}
