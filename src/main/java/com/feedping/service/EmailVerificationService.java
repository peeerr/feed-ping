package com.feedping.service;

import com.feedping.domain.Member;
import com.feedping.exception.ErrorCode;
import com.feedping.exception.GlobalException;
import com.feedping.repository.MemberRepository;
import com.feedping.util.EmailSender;
import com.feedping.util.EmailVerificationManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional
@Service
public class EmailVerificationService {

    private final MemberRepository memberRepository;
    private final EmailVerificationManager codeManager;
    private final EmailSender emailSender;
    private final AuthTokenService authTokenService;

    public void sendVerificationEmail(String email) {
        if (memberRepository.existsByEmail(email)) {
            throw new GlobalException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        String code = codeManager.createCode(email);
        emailSender.sendEmail(email, "이메일 인증", "인증 코드: " + code);
    }

    public void verifyEmail(String email, String code) {
        codeManager.validateCode(email, code);

        Member member = Member.builder()
                .email(email)
                .build();

        memberRepository.save(member);
        authTokenService.createTokenForMember(member);
    }

}
