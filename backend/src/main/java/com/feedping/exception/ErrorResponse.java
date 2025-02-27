package com.feedping.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@JsonInclude(Include.NON_NULL)
@Getter
public class ErrorResponse {

    private final int code;
    private final String message;
    private final String detail;
    private final Map<String, String> validation;

    @Builder
    public ErrorResponse(int code, String message, String detail) {
        this.code = code;
        this.message = message;
        this.detail = detail;
        this.validation = new HashMap<>();
    }

    public void addValidation(String fieldName, String errorMessage) {
        this.validation.put(fieldName, errorMessage);
    }

}
