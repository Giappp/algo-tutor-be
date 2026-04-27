package org.rap.algotutorbe.problem.domain.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProblemExample(

        @JsonProperty("input")
        String input,

        @JsonProperty("output")
        String output,

        @JsonProperty("explanation")
        String explanation,

        @JsonProperty("imageUrl")
        String imageUrl
) {
}