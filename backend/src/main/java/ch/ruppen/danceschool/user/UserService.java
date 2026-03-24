package ch.ruppen.danceschool.user;

import ch.ruppen.danceschool.schoolmember.MembershipDto;
import ch.ruppen.danceschool.schoolmember.SchoolMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SchoolMemberService schoolMemberService;

    public AppUser findOrCreateOAuthUser(String provider, String oauthId, String email, String name, String avatarUrl) {
        return userRepository.findByOauthProviderAndOauthId(provider, oauthId)
                .map(existing -> {
                    existing.setEmail(email);
                    existing.setName(name);
                    existing.setAvatarUrl(avatarUrl);
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    AppUser user = new AppUser();
                    user.setOauthProvider(provider);
                    user.setOauthId(oauthId);
                    user.setEmail(email);
                    user.setName(name);
                    user.setAvatarUrl(avatarUrl);
                    return userRepository.save(user);
                });
    }

    public Optional<AppUser> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<AppUser> findById(Long id) {
        return userRepository.findById(id);
    }

    public UserDto toUserDto(AppUser user) {
        List<MembershipDto> memberships = schoolMemberService.findMembershipsByUserId(user.getId());
        return new UserDto(user.getId(), user.getEmail(), user.getName(), user.getAvatarUrl(), memberships);
    }
}
