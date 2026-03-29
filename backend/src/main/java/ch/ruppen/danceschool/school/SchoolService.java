package ch.ruppen.danceschool.school;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SchoolService {

    private final SchoolRepository schoolRepository;
    private final SchoolMapper schoolMapper;

    public School createSchool(SchoolDto dto) {
        School school = schoolMapper.toEntity(dto);
        return schoolRepository.save(school);
    }

    public SchoolDto toDto(School school) {
        return schoolMapper.toDto(school);
    }

    public Optional<School> findByOwnerUserId(Long userId) {
        return schoolRepository.findByOwnerUserId(userId);
    }

    public School updateSchool(School school, SchoolUpdateDto dto) {
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
        return schoolRepository.save(school);
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
