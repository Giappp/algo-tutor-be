package org.rap.algotutorbe.problem.domain;

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
    private int timeLimitMs;
    private int memoryLimitMb;
    private int maxCodeLengthBytes;
    private int maxOutputSizeBytes;

    public static Constraints defaults() {
        return new Constraints(1000, 256, 65536, 1048576);
    }
}
