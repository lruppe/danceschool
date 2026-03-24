package ch.ruppen.danceschool.school;

import ch.ruppen.danceschool.config.MapStructConfig;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
interface SchoolMapper {

    SchoolDto toDto(School school);

    School toEntity(SchoolDto dto);

    void updateEntity(SchoolDto dto, @MappingTarget School school);
}
