package ch.ruppen.danceschool.shared.security;

import ch.ruppen.danceschool.schoolmember.SchoolMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Authorization bean exposed as SpEL name {@code schoolAuthz}. Controllers guard admin
 * endpoints with {@code @PreAuthorize("@schoolAuthz.hasMembership()")}.
 * <p>
 * Phase 1 treats OWNER and TEACHER identically and assumes a single {@code SchoolMember}
 * per user; the check collapses to "does the caller have any membership?". Phase 2 will
 * parameterize this (e.g., {@code isMemberOf(#schoolId)}).
 * <p>
 * The check queries {@code SchoolMemberRepository} on every call rather than trusting
 * {@code AuthenticatedUser.schoolId()} off the principal. This is load-bearing for the
 * onboarding flow under session-based dev auth: the principal is built once at login with
 * {@code schoolId = null} and never refreshed, so a caller who logs in and then creates
 * their first school in the same session still sees a stale {@code null}. Costs one extra
 * query per admin request; acceptable at current scale.
 */
@Component("schoolAuthz")
@RequiredArgsConstructor
public class SchoolAuthz {

    private final SchoolMemberService schoolMemberService;

    public boolean hasMembership() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        if (!(auth.getPrincipal() instanceof AuthenticatedUser user)) {
            return false;
        }
        return schoolMemberService.findSchoolIdByUserId(user.userId()).isPresent();
    }
}
