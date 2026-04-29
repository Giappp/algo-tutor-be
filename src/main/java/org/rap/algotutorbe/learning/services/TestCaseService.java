package org.rap.algotutorbe.learning.services;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.dto.TestCaseDTO;
import org.rap.algotutorbe.learning.dto.TestCaseResponseDTO;
import org.rap.algotutorbe.learning.mapper.TestCaseMapper;
import org.rap.algotutorbe.learning.models.CodingLesson;
import org.rap.algotutorbe.learning.models.Testcase;
import org.rap.algotutorbe.learning.repositories.CodingLessonRepository;
import org.rap.algotutorbe.learning.repositories.TestcaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TestCaseService {
    private final TestcaseRepository testcaseRepository;
    private final CodingLessonRepository codingLessonRepository;
    private final TestCaseMapper testCaseMapper;

    @Transactional
    public @Nullable ApiResponse<Object> create(Long lessonId, @Valid TestCaseDTO dto) {
        CodingLesson lesson = getCodingLessonOrThrow(lessonId);

        Testcase testCase = testCaseMapper.toEntity(dto);
        testCase.setCodingLesson(lesson);
        lesson.getTestCases().add(testCase);

        codingLessonRepository.save(lesson);
        return ApiResponse.buildSuccess(testCaseMapper.toResponse(testCase));
    }

    @Transactional
    public @Nullable ApiResponse<Object> update(Long testCaseId, @Valid TestCaseDTO dto) {
        Testcase testCase = getOrThrow(testCaseId);
        testCaseMapper.updateEntity(testCase, dto);
        Testcase saved = testcaseRepository.save(testCase);
        return ApiResponse.buildSuccess(testCaseMapper.toResponse(saved));
    }

    @Transactional
    public @Nullable ApiResponse<Object> delete(Long testCaseId) {
        Testcase testCase = getOrThrow(testCaseId);
        testcaseRepository.delete(testCase);
        return ApiResponse.buildMessage("Test case deleted successfully");
    }

    @Transactional(readOnly = true)
    public @Nullable ApiResponse<Object> getByLessonId(Long lessonId) {
        getCodingLessonOrThrow(lessonId);
        List<Testcase> testCases = testcaseRepository.findByCodingLessonId(lessonId);
        List<TestCaseResponseDTO> responses = testCases.stream()
                .map(testCaseMapper::toResponse)
                .toList();
        return ApiResponse.buildSuccess(responses);
    }

    private CodingLesson getCodingLessonOrThrow(Long lessonId) {
        return codingLessonRepository.findById(lessonId)
                .orElseThrow(() -> new AppException(ErrorCode.CODING_LESSON_REQUIRED));
    }

    private Testcase getOrThrow(Long id) {
        return testcaseRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TEST_CASE_NOT_FOUND));
    }
}
