package com.feedping.dto.request;

import static com.feedping.exception.ValidationErrorMessage.EMAIL_REGEX;
import static com.feedping.exception.ValidationErrorMessage.EMPTY_EMAIL;
import static com.feedping.exception.ValidationErrorMessage.EMPTY_SITE_NAME;
import static com.feedping.exception.ValidationErrorMessage.EMPTY_URL;
import static com.feedping.exception.ValidationErrorMessage.INVALID_EMAIL;
import static com.feedping.exception.ValidationErrorMessage.INVALID_URL;
import static com.feedping.exception.ValidationErrorMessage.MAX_LENGTH;
import static com.feedping.exception.ValidationErrorMessage.URL_MAX_LENGTH;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.hibernate.validator.constraints.URL;

@Getter
public class RssSubscriptionRequest {

    @Size(max = 255, message = MAX_LENGTH)
    @NotEmpty(message = EMPTY_EMAIL)
    @Pattern(regexp = EMAIL_REGEX, message = INVALID_EMAIL)
    private String email;

    @Size(max = 4096, message = URL_MAX_LENGTH)
    @NotEmpty(message = EMPTY_URL)
    @URL(message = INVALID_URL)
    private String rssUrl;

    @Size(max = 255, message = MAX_LENGTH)
    @NotEmpty(message = EMPTY_SITE_NAME)
    private String siteName;

}
