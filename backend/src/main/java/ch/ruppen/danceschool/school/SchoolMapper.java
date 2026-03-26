package ch.ruppen.danceschool.school;

import ch.ruppen.danceschool.config.MapStructConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
interface SchoolMapper {

    SchoolDto toDto(School school);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tagline", ignore = true)
    @Mapping(target = "about", ignore = true)
    @Mapping(target = "website", ignore = true)
    @Mapping(target = "coverImageUrl", ignore = true)
    @Mapping(target = "logoUrl", ignore = true)
    @Mapping(target = "specialties", ignore = true)
    @Mapping(target = "galleryImages", ignore = true)
    @Mapping(target = "youtubeVideos", ignore = true)
    School toEntity(SchoolDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tagline", ignore = true)
    @Mapping(target = "about", ignore = true)
    @Mapping(target = "website", ignore = true)
    @Mapping(target = "coverImageUrl", ignore = true)
    @Mapping(target = "logoUrl", ignore = true)
    @Mapping(target = "specialties", ignore = true)
    @Mapping(target = "galleryImages", ignore = true)
    @Mapping(target = "youtubeVideos", ignore = true)
    void updateEntity(SchoolDto dto, @MappingTarget School school);
}
