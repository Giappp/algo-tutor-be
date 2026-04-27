package org.rap.algotutorbe.problem.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.common.services.BaseService;
import org.rap.algotutorbe.common.services.SlugGenerator;
import org.rap.algotutorbe.problem.domain.models.Tag;
import org.rap.algotutorbe.problem.dto.TagDetailDto;
import org.rap.algotutorbe.problem.dto.TagDto;
import org.rap.algotutorbe.problem.dto.request.UpdateOrCreateTagRequest;
import org.rap.algotutorbe.problem.mapper.TagMapper;
import org.rap.algotutorbe.problem.repositories.TagRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TagService extends BaseService {
    private final TagRepository tagRepository;
    private final TagMapper tagMapper;
    private final SlugGenerator slugGenerator;

    public TagDto create(UpdateOrCreateTagRequest dto) {
        Tag tag = new Tag();
        String slug = slugGenerator.generateFrom(dto.name());
        tag.setName(dto.name());
        tag.setSlug(slug);
        tagRepository.save(tag);
        return tagMapper.toDto(tag);
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

    public ApiResponse<List<TagDetailDto>> getAllTags() {
        List<Tag> tags = tagRepository.findAll();
        List<TagDetailDto> result = tags.stream()
                .map(this::toTagDetailDto)
                .toList();
        return ApiResponse.buildSuccess(result);
    }

    public ApiResponse<TagDetailDto> getTagBySlug(String slug) {
        Tag tag = tagRepository.findBySlugWithProblems(slug)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        return ApiResponse.buildSuccess(toTagDetailDto(tag));
    }

    private TagDetailDto toTagDetailDto(Tag tag) {
        return new TagDetailDto(
                tag.getId(),
                tag.getName(),
                tag.getSlug()
        );
    }
}
