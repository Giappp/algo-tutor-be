package org.rap.algotutorbe.ai.services;

import lombok.RequiredArgsConstructor;
import org.rap.algotutorbe.common.errors.ErrorCode;
import org.rap.algotutorbe.common.exception.AppException;
import org.rap.algotutorbe.judge.PistonClient;
import org.rap.algotutorbe.judge.dto.PistonResponse;
import org.rap.algotutorbe.learning.enums.ProgrammingLanguage;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CodingCompileValidator {

    private static final int RUN_TIMEOUT_MS = 2_000;
    private static final int COMPILE_TIMEOUT_MS = 10_000;
    private static final int MEMORY_LIMIT_MB = 256;

    private final PistonClient pistonClient;

    public void validate(ProgrammingLanguage language, String sourceCode) {
        PistonResponse response = pistonClient.executeRaw(
                language,
                validationSource(language, sourceCode),
                "",
                RUN_TIMEOUT_MS,
                COMPILE_TIMEOUT_MS,
                MEMORY_LIMIT_MB);
        boolean invalidCompiledLanguage = language != ProgrammingLanguage.PYTHON
                && (response == null || response.compile() == null || response.compile().code() != 0);
        boolean invalidPython = language == ProgrammingLanguage.PYTHON
                && (response == null || response.run() == null || response.run().code() != 0);
        if (invalidCompiledLanguage || invalidPython) {
            throw new AppException(ErrorCode.AI_GENERATED_CODE_COMPILE_FAILED);
        }
    }

    public void validateAll(Map<String, String> starterCode) {
        starterCode.forEach((language, source) ->
                validate(ProgrammingLanguage.valueOf(language.toUpperCase(Locale.ROOT)), source));
    }

    private String validationSource(ProgrammingLanguage language, String sourceCode) {
        if (language == ProgrammingLanguage.CPP && !sourceCode.matches("(?s).*\\bmain\\s*\\(.*")) {
            return sourceCode + "\nint main() { return 0; }\n";
        }
        return sourceCode;
    }
}
