package org.rap.algotutorbe.problem.application.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.problem.application.dto.TagsDto;
import org.rap.algotutorbe.problem.application.dto.request.CreateTagRequest;
import org.rap.algotutorbe.problem.domain.models.Tag;
import org.rap.algotutorbe.problem.infrastructure.repositories.TagRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;

    public TagsDto create(CreateTagRequest dto) {
        Tag tag = new Tag();
        tag.setName(dto.name());
        tagRepository.save(tag);
        return new TagsDto(tag.getId(), tag.getName());
    }

    public void delete(Long id) {
        tagRepository.deleteById(id);
    }

    public void batchCreate(List<CreateTagRequest> batch) {

    }
}
