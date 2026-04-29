package org.rap.algotutorbe.learning.dto;

public record TestCaseDTO(String stdin, String expectedOutput, Boolean isHidden, Integer orderIndex,
                          String explanation) {
}
