package org.rap.algotutorbe.problem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TagCriteria(String name) {
}
