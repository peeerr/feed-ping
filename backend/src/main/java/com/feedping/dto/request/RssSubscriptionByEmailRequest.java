package com.feedping.dto.request;

import static com.feedping.exception.ValidationErrorMessage.EMPTY_SITE_NAME;
import static com.feedping.exception.ValidationErrorMessage.EMPTY_URL;
import static com.feedping.exception.ValidationErrorMessage.INVALID_URL;
import static com.feedping.exception.ValidationErrorMessage.MAX_LENGTH;
import static com.feedping.exception.ValidationErrorMessage.URL_MAX_LENGTH;

import com.feedping.exception.ValidationErrorMessage;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import org.hibernate.validator.constraints.URL;

@Getter
public class RssSubscriptionByEmailRequest {

    @NotEmpty(message = ValidationErrorMessage.EMPTY_TOKEN)
    private String token;

    @Size(max = 4096, message = URL_MAX_LENGTH)
    @NotEmpty(message = EMPTY_URL)
    @URL(message = INVALID_URL)
    private String rssUrl;

    @Size(max = 255, message = MAX_LENGTH)
    @NotEmpty(message = EMPTY_SITE_NAME)
    private String siteName;

}
