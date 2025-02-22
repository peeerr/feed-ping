package com.feedping.dto.request;

import static com.feedping.exception.ValidationErrorMessage.EMPTY_TOKEN;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

@Getter
public class RssUnsubscribeRequest {

    @NotEmpty(message = EMPTY_TOKEN)
    private String token;

}
