package ch.ruppen.danceschool.shared.security;

/**
 * Authenticated principal exposed to controllers.
 * <p>
 * {@code schoolId} is {@code null} when the user has no {@code SchoolMember} row yet
 * (needs-onboarding state). Phase 1 assumes a single school per user.
 */
public record AuthenticatedUser(Long userId, String email, Long schoolId) {
}
