package ch.ruppen.danceschool.user;

/**
 * Published by {@link UserService} every time {@code findOrCreateByFirebaseUid} resolves to a
 * user — whether the user was just created or already existed. Lets downstream features react
 * to authentication without coupling {@code UserService} to them (and avoids the circular
 * dependencies that direct injection would create).
 * <p>
 * Currently used by {@code DemoSchoolSeeder} to provision a demo school on first login —
 * including for users that pre-date demo mode being enabled. Listeners must be cheap and
 * idempotent: this fires on every authenticated request, not only on user creation.
 */
public record UserAuthenticatedEvent(Long userId) {
}
