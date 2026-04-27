package org.rap.algotutorbe.problem.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.problem.domain.models.Problem;
import org.rap.algotutorbe.problem.domain.models.Tag;
import org.rap.algotutorbe.problem.dto.TagDetailDto;
import org.rap.algotutorbe.problem.dto.TagStatsDto;
import org.rap.algotutorbe.problem.repositories.ProblemRepository;
import org.rap.algotutorbe.problem.repositories.TagRepository;

import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    @Mock
    TagRepository tagRepository;

    @Mock
    ProblemRepository problemRepository;

    @Mock
    UserProblemProgressRepository progressRepository;

    @InjectMocks
    TagService tagService;

    private Tag testTag;
    private Problem testProblem;

    @BeforeEach
    void setUp() {
        testProblem = new Problem();
        testProblem.setId(1L);
        testProblem.setTitle("Two Sum");
        testProblem.setSlug("two-sum");
        testProblem.setStatus(ProblemStatus.PUBLISHED);
        testProblem.setTags(new HashSet<>());

        testTag = new Tag();
        testTag.setId(1L);
        testTag.setName("Array");
        testTag.setSlug("array");
        testTag.setProblems(new HashSet<>());
        testTag.getProblems().add(testProblem);
        testProblem.getTags().add(testTag);
    }

    @Test
    void getTagBySlug_returnsTagWithProblemCount() {
        when(tagRepository.findBySlugWithProblems("array"))
                .thenReturn(Optional.of(testTag));

        ApiResponse<TagDetailDto> response = tagService.getTagBySlug("array");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Array", response.getData().name());
        assertEquals("array", response.getData().slug());
        assertEquals(1L, response.getData().problemCount());
    }

    @Test
    void getTagStats_returnsStatsForPublishedProblems() {
        when(tagRepository.findBySlugWithProblems("array"))
                .thenReturn(Optional.of(testTag));

        ApiResponse<TagStatsDto> response = tagService.getTagStats("array");

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals("Array", response.getData().name());
        assertEquals(1L, response.getData().totalProblems());
    }
}
