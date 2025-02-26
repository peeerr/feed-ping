package com.feedping.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Getter;

@JsonInclude(Include.NON_NULL)
@Getter
public class ApiResponse<T> {

    private int code;
    private String message;
    private T data;
    private boolean async;

    public static <T> ApiResponse<T> of(int code) {
        return new ApiResponse<>(code, "success");
    }

    public static <T> ApiResponse<T> of(int code, T data) {
        return new ApiResponse<>(code, "success", data);
    }

    public static <T> ApiResponse<T> ofAsync(int code, String message) {
        ApiResponse<T> response = new ApiResponse<>(code, message);
        response.async = true;
        return response;
    }

    public ApiResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

}
