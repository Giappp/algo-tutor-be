package org.rap.algotutorbe.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@Data
public class ApiResponse<T> {
    T data;
    private boolean success;
    private String message;
}
