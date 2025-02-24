package com.feedping.controller;

import com.feedping.dto.ApiResponse;
import com.feedping.service.AuthTokenService;
import jakarta.validation.constraints.NotEmpty;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping("/api/token")
@RestController
public class TokenController {

    private final AuthTokenService authTokenService;

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<Void>> validateToken(@RequestParam @NotEmpty String token) {
        // 토큰 유효성 검사
        authTokenService.validateAndGetMember(token);
        return ResponseEntity.ok(ApiResponse.of(HttpStatus.OK.value()));
    }

}
