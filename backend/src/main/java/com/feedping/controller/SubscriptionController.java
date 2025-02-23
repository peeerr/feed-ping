package com.feedping.controller;

import com.feedping.dto.ApiResponse;
import com.feedping.dto.request.RssSubscriptionByEmailRequest;
import com.feedping.dto.request.RssSubscriptionRequest;
import com.feedping.dto.request.RssUnsubscribeRequest;
import com.feedping.dto.response.RssSubscriptionPageResponse;
import com.feedping.exception.ErrorCode;
import com.feedping.exception.GlobalException;
import com.feedping.exception.ValidationErrorMessage;
import com.feedping.service.SubscriptionService;
import com.feedping.util.EmailVerificationManager;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/subscriptions")
@RestController
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final EmailVerificationManager emailVerificationManager;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> subscribeRss(
            @RequestBody @Valid RssSubscriptionRequest request
    ) {
        if (!emailVerificationManager.isEmailVerified(request.getEmail())) {
            throw new GlobalException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        subscriptionService.subscribeRss(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED.value()));
    }

    @GetMapping("/manage")
    public ResponseEntity<ApiResponse<RssSubscriptionPageResponse>> getSubscriptionManagementPage(
            @RequestParam @NotEmpty(message = ValidationErrorMessage.EMPTY_TOKEN) String token,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        RssSubscriptionPageResponse page = subscriptionService.getSubscriptionsByEmail(token, pageable);
        return ResponseEntity.ok()
                .body(ApiResponse.of(HttpStatus.OK.value(), page));
    }

    @PostMapping("/manage")
    public ResponseEntity<ApiResponse<Void>> subscribeRssFromEmail(
            @RequestBody @Valid RssSubscriptionByEmailRequest request
    ) {
        subscriptionService.subscribeRssWithToken(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(HttpStatus.CREATED.value()));
    }

    @DeleteMapping("/manage/{rssId}")
    public ResponseEntity<ApiResponse<Void>> unsubscribeRssFromEmail(
            @PathVariable Long rssId,
            @RequestBody @Valid RssUnsubscribeRequest request
    ) {
        subscriptionService.unsubscribeRssWithToken(rssId, request);
        return ResponseEntity.ok()
                .body(ApiResponse.of(HttpStatus.OK.value()));
    }

}
