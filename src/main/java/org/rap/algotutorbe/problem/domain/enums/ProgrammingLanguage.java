package org.rap.algotutorbe.problem.domain.enums;

import org.rap.algotutorbe.problem.domain.models.Constraints;

public enum ProgrammingLanguage {
    CPP {
        @Override
        public Constraints calculateConstraints(Constraints base) {
            return new Constraints(
                    base.getTimeLimitMs(),
                    base.getMemoryLimitMb(),
                    base.getMaxCodeLengthBytes(),
                    base.getMaxOutputSizeBytes()
            );
        }
    },
    JAVA {
        @Override
        public Constraints calculateConstraints(Constraints base) {
            return new Constraints(
                    base.getTimeLimitMs() * 2 + 500L,
                    base.getMemoryLimitMb() + 256L,
                    base.getMaxCodeLengthBytes(),
                    base.getMaxOutputSizeBytes()
            );
        }
    },
    PYTHON {
        @Override
        public Constraints calculateConstraints(Constraints base) {
            return new Constraints(
                    base.getTimeLimitMs() * 3,
                    base.getMemoryLimitMb() + 64L,
                    base.getMaxCodeLengthBytes(),
                    base.getMaxOutputSizeBytes()
            );
        }
    };

    public abstract Constraints calculateConstraints(Constraints baseConstraints);
}
