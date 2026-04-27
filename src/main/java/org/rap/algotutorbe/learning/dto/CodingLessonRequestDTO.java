package org.rap.algotutorbe.learning.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class CodingLessonRequestDTO extends LessonRequestDTO {
    private double timeLimit;
    private double memoryLimit;
    private String constraints;
    private List<TestCaseDTO> testCases;
}