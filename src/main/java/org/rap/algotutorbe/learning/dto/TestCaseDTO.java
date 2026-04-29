package org.rap.algotutorbe.learning.dto;

public record TestCaseDTO(String stdin, String expectedStdout, Boolean isHidden, Integer orderIndex,
                          String explanation) {
}
