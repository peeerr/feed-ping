package com.feedping.exception;

import static com.feedping.exception.ValidationErrorMessage.VALIDATION_ERROR;

import jakarta.validation.ConstraintViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> invalid(MethodArgumentNotValidException e) {
        ErrorResponse errorResponse = getErrorResponse(400, VALIDATION_ERROR, null);

        e.getFieldErrors().forEach(filedError ->
                errorResponse.addValidation(filedError.getField(), filedError.getDefaultMessage()));

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        ErrorResponse errorResponse = getErrorResponse(400, VALIDATION_ERROR, null);

        e.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            errorResponse.addValidation(fieldName, violation.getMessage());
        });

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
        ErrorResponse errorResponse = getErrorResponse(400, "필수 파라미터가 누락되었습니다.", null);
        errorResponse.addValidation(e.getParameterName(), "이 값은 필수입니다.");

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        ErrorResponse errorResponse = getErrorResponse(400, "잘못된 요청 형식입니다.", null);

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<ErrorResponse> handlerCustomException(GlobalException e) {
        HttpStatus status = e.getErrorCode().getStatus();

        ErrorResponse errorResponse = getErrorResponse(status.value(), e.getMessage(), e.getDetail());

        return ResponseEntity.status(status).body(errorResponse);
    }

    private ErrorResponse getErrorResponse(int code, String message, String detail) {
        return ErrorResponse.builder()
                .code(code)
                .message(message)
                .detail(detail)
                .build();
    }

}
