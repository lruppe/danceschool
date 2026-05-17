package ch.ruppen.danceschool.user;

/**
 * Published by {@link UserService} immediately after a brand-new {@link AppUser} is persisted.
 * Lets downstream features (e.g. demo seeding) react to user onboarding without {@code UserService}
 * needing to know about them — avoids tying low-level user creation into higher-level services
 * and the circular dependencies that would create.
 */
public record UserCreatedEvent(Long userId) {
}
