package org.rap.algotutorbe.learning.dto;

public record TopicRequestDTO(String name, String description, String scopeTags,
                              Boolean isLocked) {
}
