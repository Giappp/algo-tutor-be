package org.rap.algotutorbe.common.api;

public record JsonNodeDto(Object raw) {
    public static JsonNodeDto of(com.fasterxml.jackson.databind.JsonNode node) {
        return node == null ? null : new JsonNodeDto(node);
    }
}
