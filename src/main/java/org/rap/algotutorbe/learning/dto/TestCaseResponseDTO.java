package org.rap.algotutorbe.learning.dto;

public record TestCaseResponseDTO(
        Long id,
        String stdin,
        String expectedStdout,
        boolean isHidden,
        int orderIndex,
        String explanation
) {
}
