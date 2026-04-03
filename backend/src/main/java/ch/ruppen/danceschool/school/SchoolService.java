package ch.ruppen.danceschool.school;

import ch.ruppen.danceschool.shared.storage.ImageStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    private final SchoolMapper schoolMapper;
    private final ImageStorageService imageStorageService;

    public School createSchool(SchoolDto dto) {
        School school = schoolMapper.toEntity(dto);
        return schoolRepository.save(school);
    }

    public School createSchoolFull(SchoolUpdateDto dto) {
        School school = new School();
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
        School saved = schoolRepository.save(school);
        replaceSpecialties(saved, dto.specialties());
        replaceGalleryImages(saved, dto.galleryImages());
        replaceYoutubeVideos(saved, dto.youtubeVideos());
        return schoolRepository.save(saved);
    }

    public SchoolDto toDto(School school) {
        return schoolMapper.toDto(school);
    }

    public Optional<School> findByOwnerUserId(Long userId) {
        return schoolRepository.findByOwnerUserId(userId);
    }

    public School updateSchool(School school, SchoolUpdateDto dto) {
        deleteReplacedImages(school, dto);

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
        return schoolRepository.save(school);
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

    public SchoolDetailDto toDetailDto(School school) {
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
