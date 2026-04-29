package org.rap.algotutorbe.learning.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.rap.algotutorbe.learning.models.ProblemExample;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class CodingLessonRequestDTO extends LessonRequestDTO {
    private Integer timeLimit;
    private Integer memoryLimit;
    private String constraints;
    private Map<String, String> starterCode;
    private List<TestCaseDTO> testCases;
    private List<ProblemExample> examples;
    private List<String> hints;
    private List<String> keyInsights;
}