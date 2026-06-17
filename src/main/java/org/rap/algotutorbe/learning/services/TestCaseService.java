package org.rap.algotutorbe.learning.services;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.api.ApiResponse;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.learning.dto.testcase.SaveTestCaseRequest;
import org.rap.algotutorbe.learning.dto.testcase.TestCaseDTO;
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
    public Testcase saveTestCase(Long problemId, TestCaseDTO request) {
        CodingLesson codingLesson = codingLessonRepository.findById(problemId)
                .orElseThrow(() -> new AppException(ErrorCode.LESSON_NOT_FOUND));

        Testcase testCase = new Testcase();
        testCase.setCodingLesson(codingLesson);
        testCase.setInputFileUrl(request.inputFileUrl());
        testCase.setOutputFileUrl(request.outputFileUrl());
        testCase.setScoreWeight(request.scoreWeight());
        testCase.setIsSample(request.isSample());
        testCase.setSortOrder(request.sortOrder());

        return testcaseRepository.save(testCase);
    }

    @Transactional
    public ApiResponse<TestCaseDTO> create(Long lessonId, @Valid SaveTestCaseRequest dto) {
        CodingLesson lesson = getCodingLessonOrThrow(lessonId);
        Testcase testCase = testCaseMapper.toEntity(dto);
        testCase.setCodingLesson(lesson);
        lesson.getTestCases().add(testCase);

        codingLessonRepository.save(lesson);
        return ApiResponse.buildSuccess(testCaseMapper.toDto(testCase));
    }

    @Transactional
    public ApiResponse<TestCaseDTO> update(Long testCaseId, @Valid SaveTestCaseRequest request) {
        Testcase testCase = getOrThrow(testCaseId);
        testCaseMapper.updateEntity(testCase, request);

        Testcase saved = testcaseRepository.save(testCase);
        return ApiResponse.buildSuccess(testCaseMapper.toDto(saved));
    }

    @Transactional
    public ApiResponse<Object> delete(Long testCaseId) {
        Testcase testCase = getOrThrow(testCaseId);
        testcaseRepository.delete(testCase);
        return ApiResponse.buildMessage("Test case deleted successfully");
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<TestCaseDTO>> getByLessonId(Long lessonId) {
        getCodingLessonOrThrow(lessonId);
        List<Testcase> testCases = testcaseRepository.findByCodingLessonId(lessonId);
        List<TestCaseDTO> responses = testCases.stream()
                .map(testCaseMapper::toDto)
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
