package com.feedping.service;

import com.feedping.domain.AuthToken;
import com.feedping.domain.Member;
import com.feedping.repository.AuthTokenRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AuthTokenService {

    private final AuthTokenRepository authTokenRepository;

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
