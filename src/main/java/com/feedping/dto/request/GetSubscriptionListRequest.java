package com.feedping.dto.request;

import com.feedping.exception.ValidationErrorMessage;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

@Getter
public class GetSubscriptionListRequest {

    @NotEmpty(message = ValidationErrorMessage.EMPTY_TOKEN)
    private String token;

}
