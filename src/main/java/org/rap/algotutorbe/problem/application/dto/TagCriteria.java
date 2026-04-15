package org.rap.algotutorbe.problem.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TagCriteria(String name) {
}
