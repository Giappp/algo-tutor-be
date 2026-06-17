package org.rap.algotutorbe.judge;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.rap.algotutorbe.common.services.S3Service;
import org.rap.algotutorbe.iam.domain.repositories.UserRepository;
import org.rap.algotutorbe.judge.dto.JudgeRequest;
import org.rap.algotutorbe.judge.dto.JudgeResponse;
import org.rap.algotutorbe.judge.dto.PistonResponse;
import org.rap.algotutorbe.judge.dto.PistonStage;
import org.rap.algotutorbe.learning.models.CodingLesson;
import org.rap.algotutorbe.learning.models.Testcase;
import org.rap.algotutorbe.learning.repositories.CodingLessonRepository;
import org.rap.algotutorbe.learning.services.LessonProgressUpdater;
import org.rap.algotutorbe.submission.entities.Verdict;
import org.rap.algotutorbe.submission.repositories.SubmissionDetailRepository;
import org.rap.algotutorbe.submission.repositories.SubmissionRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JudgeServiceTest {

    @TempDir
    Path tempDir;

    private CodingLessonRepository codingLessonRepository;
    private PistonClient pistonClient;
    private S3Service s3Service;
    private JudgeService judgeService;
    private CodingLesson lesson;

    @BeforeEach
    void setUp() {
        codingLessonRepository = mock(CodingLessonRepository.class);
        pistonClient = mock(PistonClient.class);
        s3Service = mock(S3Service.class);
        judgeService = new JudgeService(
                codingLessonRepository,
                pistonClient,
                mock(UserRepository.class),
                mock(LessonProgressUpdater.class),
                mock(SubmissionRepository.class),
                mock(SubmissionDetailRepository.class),
                mock(SimpMessagingTemplate.class),
                s3Service,
                Runnable::run
        );

        lesson = new CodingLesson();
        lesson.setId(1L);
        lesson.setBaseTimeLimitMs(2_000);
        lesson.setBaseMemoryLimitMb(256);
        when(codingLessonRepository.findBySlugWithTestCases("two-sum")).thenReturn(Optional.of(lesson));
    }

    @Test
    void run_returnsStdoutStderrAndExecutesAllSampleTestCases() throws Exception {
        lesson.setTestCases(List.of(sampleTestcase(1, "1", "expected-1"), sampleTestcase(2, "2", "expected-2")));
        when(pistonClient.executeRaw(any(), any(), any(), any(Integer.class), any(Integer.class), any(Integer.class)))
                .thenReturn(response(runStage("wrong", "warning", 0, null, 1_024, 10)))
                .thenReturn(response(runStage("expected-2", "", 0, null, 2_048, 20)));

        JudgeResponse result = judgeService.run(new JudgeRequest("two-sum", "python", "print('x')"));

        assertThat(result.verdict()).isEqualTo(Verdict.WRONG_ANSWER.name());
        assertThat(result.summary().executed()).isEqualTo(2);
        assertThat(result.summary().failed()).isEqualTo(1);
        assertThat(result.testCases()).hasSize(2);
        assertThat(result.testCases().getFirst().stdout()).isEqualTo("wrong");
        assertThat(result.testCases().getFirst().stderr()).isEqualTo("warning");
        assertThat(result.testCases().getFirst().memoryKb()).isEqualTo(1);
    }

    @Test
    void run_returnsCompilationErrorFromPiston() throws Exception {
        lesson.setTestCases(List.of(sampleTestcase(1, "", "")));
        PistonStage compile = new PistonStage("", "Main.java: error", "", 1, null, null, null, null);
        when(pistonClient.executeRaw(any(), any(), any(), any(Integer.class), any(Integer.class), any(Integer.class)))
                .thenReturn(new PistonResponse("java", "*", compile, null));

        JudgeResponse result = judgeService.run(new JudgeRequest("two-sum", "java", "broken"));

        assertThat(result.verdict()).isEqualTo(Verdict.COMPILATION_ERROR.name());
        assertThat(result.compilationError()).isEqualTo("Main.java: error");
        assertThat(result.testCases().getFirst().stderr()).isEqualTo("Main.java: error");
    }

    @Test
    void run_mapsPistonTimeoutSignalToTimeLimitExceeded() throws Exception {
        lesson.setTestCases(List.of(sampleTestcase(1, "", "")));
        when(pistonClient.executeRaw(any(), any(), any(), any(Integer.class), any(Integer.class), any(Integer.class)))
                .thenReturn(response(runStage("", "process timed out", null, "SIGKILL", 1_024, 3_000)));

        JudgeResponse result = judgeService.run(new JudgeRequest("two-sum", "cpp", "while(true) {}"));

        assertThat(result.verdict()).isEqualTo(Verdict.TIME_LIMIT_EXCEEDED.name());
        assertThat(result.testCases().getFirst().stderr()).isEqualTo("process timed out");
    }

    private Testcase sampleTestcase(int index, String input, String expected) throws Exception {
        Path inputPath = tempDir.resolve("input-" + index + ".txt");
        Path outputPath = tempDir.resolve("output-" + index + ".txt");
        Files.writeString(inputPath, input);
        Files.writeString(outputPath, expected);

        Testcase testcase = new Testcase();
        testcase.setId((long) index);
        testcase.setIsSample(true);
        testcase.setSortOrder(index);
        testcase.setInputFileUrl("input-" + index);
        testcase.setOutputFileUrl("output-" + index);

        when(s3Service.getOrCreateLocalTestCaseFile(lesson.getId(), testcase.getInputFileUrl())).thenReturn(inputPath);
        when(s3Service.getOrCreateLocalTestCaseFile(lesson.getId(), testcase.getOutputFileUrl())).thenReturn(outputPath);
        return testcase;
    }

    private PistonResponse response(PistonStage run) {
        return new PistonResponse("python", "*", null, run);
    }

    private PistonStage runStage(
            String stdout,
            String stderr,
            Integer code,
            String signal,
            Integer memoryBytes,
            Integer cpuTimeMs
    ) {
        return new PistonStage(stdout, stderr, stdout + stderr, code, signal, memoryBytes, cpuTimeMs, cpuTimeMs);
    }
}
