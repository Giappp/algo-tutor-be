package org.rap.algotutorbe.learning.dto.landing;

/**
 * Public-safe response DTO for test cases.
 * Excludes expectedStdout for hidden test cases.
 */
public record PublicTestCaseResponseDTO(
        Long id,
        String stdin,
        String expectedStdout,
        boolean isHidden,
        int orderIndex,
        String explanation
) {
    /**
     * Only show expected output for visible (non-hidden) test cases.
     */
    public String expectedStdout() {
        return isHidden ? null : expectedStdout;
    }
}
