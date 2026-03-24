package ch.ruppen.danceschool.school;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
