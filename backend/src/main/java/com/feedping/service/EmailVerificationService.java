package com.feedping.service;

import com.feedping.domain.Member;
import com.feedping.exception.ErrorCode;
import com.feedping.exception.GlobalException;
import com.feedping.repository.MemberRepository;
import com.feedping.util.EmailSender;
import com.feedping.util.EmailVerificationManager;
import java.util.Optional;
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
        String code = codeManager.createCode(email);
        emailSender.sendVerificationEmail(email, code);
    }

    public void verifyEmail(String email, String code) {
        codeManager.validateCode(email, code);

        Optional<Member> optionalMember = memberRepository.findByEmail(email);

        if (optionalMember.isEmpty()) {
            Member newMember = Member.builder()
                    .email(email)
                    .build();
            memberRepository.save(newMember);
            authTokenService.createTokenForMember(newMember);
        }
    }

}
