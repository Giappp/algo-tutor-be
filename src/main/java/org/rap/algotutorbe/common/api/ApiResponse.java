package org.rap.algotutorbe.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private T data;
    private Object meta;
    private String message;
    private boolean success;

    public static <T> ApiResponse<T> buildSuccess(T data) {
        return ApiResponse.<T>builder()
                .data(data)
                .success(true)
                .build();
    }

    public static <T> ApiResponse<T> buildSuccess(T data, Object meta) {
        return ApiResponse.<T>builder()
                .data(data)
                .meta(meta)
                .success(true)
                .build();
    }

    public static <T> ApiResponse<T> buildMessage(String message) {
        return ApiResponse.<T>builder()
                .message(message)
                .success(true)
                .build();
    }
}