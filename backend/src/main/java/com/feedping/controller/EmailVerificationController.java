package com.feedping.controller;

import com.feedping.dto.ApiResponse;
import com.feedping.dto.request.EmailVerificationSendRequest;
import com.feedping.dto.request.EmailVerificationConfirmRequest;
import com.feedping.service.EmailVerificationService;
import com.feedping.util.EmailVerificationManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/email-verification")
@RestController
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;
    private final EmailVerificationManager emailVerificationManager;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> sendVerificationEmail(@RequestBody @Valid EmailVerificationSendRequest request) {
        emailVerificationService.sendVerificationEmail(request.getEmail());
        return ResponseEntity.ok().body(ApiResponse.of(HttpStatus.OK.value()));
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(@RequestBody @Valid EmailVerificationConfirmRequest request) {
        emailVerificationService.verifyEmail(request.getEmail(), request.getCode());
        return ResponseEntity.ok().body(ApiResponse.of(HttpStatus.OK.value()));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Boolean>> checkAuthStatus(@RequestParam String email) {
        boolean isVerified = emailVerificationManager.isEmailVerified(email);
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value(), isVerified));
    }

}
