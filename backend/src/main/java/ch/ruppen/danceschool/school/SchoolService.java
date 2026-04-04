package ch.ruppen.danceschool.school;

import ch.ruppen.danceschool.schoolmember.MemberRole;
import ch.ruppen.danceschool.schoolmember.SchoolMember;
import ch.ruppen.danceschool.schoolmember.SchoolMemberService;
import ch.ruppen.danceschool.shared.error.ResourceNotFoundException;
import ch.ruppen.danceschool.shared.storage.ImageStorageService;
import ch.ruppen.danceschool.user.AppUser;
import ch.ruppen.danceschool.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolService {

    private final SchoolRepository schoolRepository;
    private final ImageStorageService imageStorageService;
    private final SchoolMemberService schoolMemberService;
    private final UserService userService;

    @Transactional
    public SchoolDetailDto createSchool(SchoolUpdateDto dto, Long userId) {
        AppUser user = userService.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        School school = new School();
        applyDto(school, dto);
        School saved = schoolRepository.save(school);

        SchoolMember member = new SchoolMember();
        member.setUser(user);
        member.setSchool(saved);
        member.setRole(MemberRole.OWNER);
        schoolMemberService.createMembership(member);

        return toDetailDto(saved);
    }

    public boolean hasSchoolByOwner(Long userId) {
        return schoolRepository.findByOwnerUserId(userId).isPresent();
    }

    public SchoolDetailDto getByOwnerUserId(Long userId) {
        return toDetailDto(findSchoolByOwner(userId));
    }

    public SchoolDetailDto updateSchool(Long userId, SchoolUpdateDto dto) {
        School school = findSchoolByOwner(userId);
        deleteReplacedImages(school, dto);
        applyDto(school, dto);
        return toDetailDto(schoolRepository.save(school));
    }

    public School findSchoolByOwner(Long userId) {
        return schoolRepository.findByOwnerUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("School", userId));
    }

    private void applyDto(School school, SchoolUpdateDto dto) {
        school.setName(dto.name());
        school.setTagline(dto.tagline());
        school.setAbout(dto.about());
        school.setStreetAddress(dto.streetAddress());
        school.setCity(dto.city());
        school.setPostalCode(dto.postalCode());
        school.setCountry(dto.country());
        school.setPhone(dto.phone());
        school.setEmail(dto.email());
        school.setWebsite(dto.website());
        school.setCoverImageUrl(dto.coverImageUrl());
        school.setLogoUrl(dto.logoUrl());
        replaceSpecialties(school, dto.specialties());
        replaceGalleryImages(school, dto.galleryImages());
        replaceYoutubeVideos(school, dto.youtubeVideos());
    }

    private void deleteReplacedImages(School school, SchoolUpdateDto dto) {
        deleteImageIfChanged(school.getCoverImageUrl(), dto.coverImageUrl());
        deleteImageIfChanged(school.getLogoUrl(), dto.logoUrl());
        deleteRemovedGalleryImages(school, dto.galleryImages());
    }

    private void deleteImageIfChanged(String oldUrl, String newUrl) {
        if (oldUrl != null && !oldUrl.isBlank() && !Objects.equals(oldUrl, newUrl)) {
            deleteImageSafely(oldUrl);
        }
    }

    private void deleteRemovedGalleryImages(School school, List<SchoolUpdateDto.GalleryImageDto> newImages) {
        Set<String> newUrls = newImages != null
                ? newImages.stream().map(SchoolUpdateDto.GalleryImageDto::url).collect(Collectors.toSet())
                : Set.of();

        for (SchoolGalleryImage existing : school.getGalleryImages()) {
            if (!newUrls.contains(existing.getUrl())) {
                deleteImageSafely(existing.getUrl());
            }
        }
    }

    // Intentionally best-effort: image cleanup failure must not block school updates.
    // Orphaned images in storage are acceptable; they can be garbage-collected later.
    private void deleteImageSafely(String url) {
        try {
            String key = ImageStorageService.extractKey(url);
            if (key != null) {
                imageStorageService.delete(key);
            }
        } catch (Exception e) {
            log.warn("Failed to delete old image from storage: {}. Error: {}", url, e.getMessage());
        }
    }

    private void replaceSpecialties(School school, List<String> specialties) {
        school.getSpecialties().clear();
        if (specialties != null) {
            for (String name : specialties) {
                var specialty = new SchoolSpecialty();
                specialty.setSchool(school);
                specialty.setName(name);
                school.getSpecialties().add(specialty);
            }
        }
    }

    private void replaceGalleryImages(School school, List<SchoolUpdateDto.GalleryImageDto> images) {
        school.getGalleryImages().clear();
        if (images != null) {
            for (var dto : images) {
                var image = new SchoolGalleryImage();
                image.setSchool(school);
                image.setUrl(dto.url());
                image.setPosition(dto.position());
                school.getGalleryImages().add(image);
            }
        }
    }

    private void replaceYoutubeVideos(School school, List<SchoolUpdateDto.YoutubeVideoDto> videos) {
        school.getYoutubeVideos().clear();
        if (videos != null) {
            for (var dto : videos) {
                var video = new SchoolYoutubeVideo();
                video.setSchool(school);
                video.setUrl(dto.url());
                video.setPosition(dto.position());
                school.getYoutubeVideos().add(video);
            }
        }
    }

    private SchoolDetailDto toDetailDto(School school) {
        return new SchoolDetailDto(
                school.getId(),
                school.getName(),
                school.getTagline(),
                school.getAbout(),
                school.getStreetAddress(),
                school.getCity(),
                school.getPostalCode(),
                school.getCountry(),
                school.getPhone(),
                school.getEmail(),
                school.getWebsite(),
                school.getCoverImageUrl(),
                school.getLogoUrl(),
                school.getSpecialties().stream()
                        .map(SchoolSpecialty::getName)
                        .toList(),
                school.getGalleryImages().stream()
                        .map(img -> new SchoolDetailDto.GalleryImageDto(img.getUrl(), img.getPosition()))
                        .toList(),
                school.getYoutubeVideos().stream()
                        .map(vid -> new SchoolDetailDto.YoutubeVideoDto(vid.getUrl(), vid.getPosition()))
                        .toList()
        );
    }
}
