package org.rap.algotutorbe.problem.application.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.problem.application.dto.TagDto;
import org.rap.algotutorbe.problem.application.dto.request.UpdateOrCreateTagRequest;
import org.rap.algotutorbe.problem.domain.models.Tag;
import org.rap.algotutorbe.problem.domain.repositories.TagRepository;
import org.rap.algotutorbe.problem.mapper.TagMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService {
    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    public TagDto create(UpdateOrCreateTagRequest dto) {
        Tag tag = new Tag();
        tag.setName(dto.name());
        tagRepository.save(tag);
        return new TagDto(tag.getId(), tag.getName(), null);
    }

    public TagDto update(Long id, UpdateOrCreateTagRequest dto) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CONFLICT_RESOURCE));
        tag.setName(dto.name());
        tagRepository.save(tag);
        return new TagDto(tag.getId(), tag.getName(), null);
    }


    public List<TagDto> getTags(String keyword) {
        List<Tag> tags = tagRepository.findAllWithKeyword(keyword);
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        return tags.stream()
                .map(tagMapper::toDto)
                .toList();
    }
}
