package ch.ruppen.danceschool.user;

import ch.ruppen.danceschool.schoolmember.MembershipDto;
import ch.ruppen.danceschool.schoolmember.SchoolMemberService;
import ch.ruppen.danceschool.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public Optional<AppUser> findByFirebaseUid(String firebaseUid) {
        return userRepository.findByFirebaseUid(firebaseUid);
    }

    @Transactional
    public AppUser findOrCreateByFirebaseUid(String firebaseUid, String email, String name) {
        return userRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> {
                    AppUser user = new AppUser();
                    user.setFirebaseUid(firebaseUid);
                    user.setEmail(email);
                    user.setName(name);
                    AppUser saved = userRepository.save(user);
                    businessLog.info("event=UserOnboarded userId={} email=\"{}\"", saved.getId(), email);
                    return saved;
                });
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
