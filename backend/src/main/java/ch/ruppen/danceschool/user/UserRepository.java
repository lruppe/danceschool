package ch.ruppen.danceschool.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface UserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByFirebaseUid(String firebaseUid);

    Optional<AppUser> findByEmail(String email);
}
