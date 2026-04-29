package org.rap.algotutorbe.learning.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.rap.algotutorbe.learning.models.ProblemExample;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class CodingLessonRequestDTO extends LessonRequestDTO {
    @Min(1)
    @Max(300000)
    private Integer timeLimit;

    @Min(1)
    @Max(1024)
    private Integer memoryLimit;

    private String constraints;

    @Size(max = 10)
    private Map<String, String> starterCode;

    @Size(max = 50)
    private List<TestCaseDTO> testCases;

    @Size(max = 5)
    private List<ProblemExample> examples;

    @Size(max = 10)
    private List<String> hints;

    @Size(max = 20)
    private List<String> keyInsights;
}