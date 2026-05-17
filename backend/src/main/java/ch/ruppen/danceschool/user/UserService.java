package ch.ruppen.danceschool.user;

import ch.ruppen.danceschool.schoolmember.MembershipDto;
import ch.ruppen.danceschool.schoolmember.SchoolMemberService;
import ch.ruppen.danceschool.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    // Dedicated business logger — findOrCreateByFirebaseUid fires on every request, so
    // @BusinessOperation can't be used (it would log on every call, not only on create).
    private static final Logger businessLog = LoggerFactory.getLogger("business");

    private final UserRepository userRepository;
    private final SchoolMemberService schoolMemberService;
    private final ApplicationEventPublisher events;

    public Optional<AppUser> findByFirebaseUid(String firebaseUid) {
        return userRepository.findByFirebaseUid(firebaseUid);
    }

    @Transactional
    public AppUser findOrCreateByFirebaseUid(String firebaseUid, String email, String name) {
        AppUser user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> {
                    AppUser fresh = new AppUser();
                    fresh.setFirebaseUid(firebaseUid);
                    fresh.setEmail(email);
                    fresh.setName(name);
                    AppUser saved = userRepository.save(fresh);
                    businessLog.info("event=UserOnboarded userId={} email=\"{}\"", saved.getId(), email);
                    return saved;
                });
        // Fired for every authenticated resolution (new or existing) so demo seeding can
        // retroactively populate users created before demo mode was enabled.
        events.publishEvent(new UserAuthenticatedEvent(user.getId()));
        return user;
    }

    public Optional<AppUser> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<AppUser> findById(Long id) {
        return userRepository.findById(id);
    }

    public UserDto getMe(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        List<MembershipDto> memberships = schoolMemberService.findMembershipsByUserId(user.getId());
        return new UserDto(user.getId(), user.getEmail(), user.getName(), user.getAvatarUrl(), memberships);
    }
}
