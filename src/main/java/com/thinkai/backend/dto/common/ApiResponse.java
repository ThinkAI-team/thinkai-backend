package com.thinkai.backend.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int status;
    private String message;
    private T data;
    private List<String> errors;

    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder().status(200).message(message).data(data).build();
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder().status(200).message("Success").data(data).build();
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return ApiResponse.<T>builder().status(201).message(message).data(data).build();
    }

    public static <T> ApiResponse<T> error(int status, String message) {
        return ApiResponse.<T>builder().status(status).message(message).build();
    }

    public static <T> ApiResponse<T> error(int status, String message, List<String> errors) {
        return ApiResponse.<T>builder().status(status).message(message).errors(errors).build();
    }
}
