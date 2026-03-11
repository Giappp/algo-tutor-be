package org.rap.algotutorbe.problem.application.services;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.rap.algotutorbe.problem.application.dto.CreateProblemDto;
import org.rap.algotutorbe.problem.application.dto.ProblemDto;
import org.rap.algotutorbe.problem.application.dto.TagsDto;
import org.rap.algotutorbe.problem.domain.Problem;
import org.rap.algotutorbe.problem.domain.ProblemTag;
import org.rap.algotutorbe.problem.domain.enums.Difficulty;
import org.rap.algotutorbe.problem.domain.enums.ProblemStatus;
import org.rap.algotutorbe.problem.infrastructure.repositories.ProblemRepository;
import org.rap.algotutorbe.problem.infrastructure.repositories.TagRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = lombok.AccessLevel.PRIVATE)
public class ProblemService {
    ProblemRepository problemRepository;
    TagRepository tagRepository;

    public ProblemDto createProblem(CreateProblemDto dto) {
        validate(dto);
        Problem problem = mapToEntity(dto);
        problemRepository.save(problem);
        return new ProblemDto(problem);
    }

    private Problem mapToEntity(CreateProblemDto dto) {
        Problem problem = new Problem();
        problem.setSlug(dto.slug());
        problem.setTitle(dto.title());
        problem.setStatement(dto.statement());
        problem.setDifficulty(Difficulty.valueOf(dto.difficulty()));
        problem.setStatus(ProblemStatus.valueOf(dto.status()));
        dto.tags().forEach(t -> {
            var tag = mapToTagEntity(t);
            problem.addTag(tag);
        });
        return problem;
    }

    private ProblemTag mapToTagEntity(TagsDto dto) {
        return tagRepository.findById(dto.id()).orElseThrow(() -> new IllegalArgumentException("Tag not found"));
    }

    private void validate(CreateProblemDto dto) {
        if (problemRepository.existsBySlug(dto.slug())) {
            throw new IllegalArgumentException("Slug already exists");
        }
        if (dto.title() == null || dto.title().isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (dto.difficulty() == null || dto.difficulty().isBlank()) {
            throw new IllegalArgumentException("Difficulty is required");
        }
        if (dto.status() == null || dto.status().isBlank()) {
            throw new IllegalArgumentException("Status is required");
        }
        if (dto.tags() == null || dto.tags().isEmpty()) {
            throw new IllegalArgumentException("Tags is required");
        }
    }
}
