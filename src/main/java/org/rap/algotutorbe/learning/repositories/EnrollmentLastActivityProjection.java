package org.rap.algotutorbe.learning.repositories;

import java.time.Instant;
import java.util.UUID;

public interface EnrollmentLastActivityProjection {
    UUID getEnrollmentId();

    Instant getLastActivityAt();
}