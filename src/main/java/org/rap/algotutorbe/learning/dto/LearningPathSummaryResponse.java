package org.rap.algotutorbe.learning.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.rap.algotutorbe.learning.models.Level;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LearningPathSummaryResponse(
    Long id,
    String name,
    String slug,
    Level level,
    String description,
    long totalTopics,
    long totalLessons,
    Instant createdAt,
    Instant updatedAt
) {}
