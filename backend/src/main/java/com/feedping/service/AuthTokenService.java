package com.feedping.service;

import com.feedping.domain.AuthToken;
import com.feedping.domain.Member;
import com.feedping.exception.ErrorCode;
import com.feedping.exception.GlobalException;
import com.feedping.repository.AuthTokenRepository;
import com.feedping.repository.MemberRepository;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AuthTokenService {

    private final AuthTokenRepository authTokenRepository;
    private final MemberRepository memberRepository;

    public String createTokenForEmail(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new GlobalException(ErrorCode.NOT_FOUND_MEMBER));

        // 기존 토큰이 있다면 재사용
        return authTokenRepository.findByMember(member)
                .map(AuthToken::getToken)
                .orElseGet(() -> generateNewToken(member));
    }

    private String generateNewToken(Member member) {
        String token = generateSecureToken();

        AuthToken authToken = AuthToken.builder()
                .token(token)
                .member(member)
                .build();

        authTokenRepository.save(authToken);
        return token;
    }

    private String generateSecureToken() {
        // UUID보다 더 짧고 URL-safe한 토큰 생성
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public Member validateAndGetMember(String token) {
        return authTokenRepository.findByToken(token)
                .map(AuthToken::getMember)
                .orElseThrow(() -> new GlobalException(ErrorCode.INVALID_TOKEN));
    }

    public void createTokenForMember(Member member) {
        if (authTokenRepository.existsByMember(member)) {
            return;
        }

        String token = UUID.randomUUID().toString();
        AuthToken authToken = AuthToken.builder()
                .token(token)
                .member(member)
                .build();
        authTokenRepository.save(authToken);
    }

}
