package com.feedping.dto.request;

import static com.feedping.exception.ValidationErrorMessage.EMAIL_REGEX;
import static com.feedping.exception.ValidationErrorMessage.EMPTY_EMAIL;
import static com.feedping.exception.ValidationErrorMessage.INVALID_EMAIL;
import static com.feedping.exception.ValidationErrorMessage.MAX_LENGTH;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class EmailVerificationSendRequest {

    @Size(max = 255, message = MAX_LENGTH)
    @NotEmpty(message = EMPTY_EMAIL)
    @Pattern(regexp = EMAIL_REGEX, message = INVALID_EMAIL)
    private String email;

}
