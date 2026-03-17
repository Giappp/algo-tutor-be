package org.rap.algotutorbe.problem.domain.models;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Constraints {
    private Long timeLimitMs;
    private Long memoryLimitMb;
    private Long maxCodeLengthBytes;
    private Long maxOutputSizeBytes;

    public static Constraints defaults() {
        return new Constraints(1000L, 256L, 65536L, 1048576L);
    }
}
