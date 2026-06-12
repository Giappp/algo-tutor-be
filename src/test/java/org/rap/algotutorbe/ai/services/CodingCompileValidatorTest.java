package org.rap.algotutorbe.ai.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.judge.PistonClient;
import org.rap.algotutorbe.judge.dto.PistonResponse;
import org.rap.algotutorbe.judge.dto.PistonStage;
import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CodingCompileValidatorTest {

    @Mock
    private PistonClient pistonClient;

    @Test
    void validate_shouldAddMainWrapperForCppScaffold() {
        PistonStage success = new PistonStage("", "", "", 0, null, null, null, null);
        when(pistonClient.executeRaw(eq(ProgrammingLanguage.CPP), org.mockito.ArgumentMatchers.anyString(),
                eq(""), anyInt(), anyInt(), anyInt()))
                .thenReturn(new PistonResponse("cpp", "*", success, success));
        CodingCompileValidator validator = new CodingCompileValidator(pistonClient);

        validator.validate(ProgrammingLanguage.CPP, "class Solution {};");

        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(pistonClient).executeRaw(eq(ProgrammingLanguage.CPP), code.capture(),
                eq(""), anyInt(), anyInt(), anyInt());
        assertThat(code.getValue()).contains("int main()");
    }

    @Test
    void validate_shouldReturnClearCompileFailure() {
        PistonStage failure = new PistonStage("", "compile error", "", 1, null, null, null, null);
        when(pistonClient.executeRaw(eq(ProgrammingLanguage.JAVA), eq("class Main {"),
                eq(""), anyInt(), anyInt(), anyInt()))
                .thenReturn(new PistonResponse("java", "*", failure, null));
        CodingCompileValidator validator = new CodingCompileValidator(pistonClient);

        assertThatThrownBy(() -> validator.validate(ProgrammingLanguage.JAVA, "class Main {"))
                .isInstanceOf(AppException.class)
                .extracting("error")
                .isEqualTo(ErrorCode.AI_GENERATED_CODE_COMPILE_FAILED);
    }
}
