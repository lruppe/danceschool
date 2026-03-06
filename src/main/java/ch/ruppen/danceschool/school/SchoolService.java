package ch.ruppen.danceschool.school;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SchoolService {

    private final SchoolRepository schoolRepository;
    private final SchoolMapper schoolMapper;

    public List<SchoolDto> findAll() {
        return schoolRepository.findAll().stream()
                .map(schoolMapper::toDto)
                .toList();
    }

    public SchoolDto create(SchoolDto dto) {
        School school = schoolMapper.toEntity(dto);
        School saved = schoolRepository.save(school);
        return schoolMapper.toDto(saved);
    }
}
