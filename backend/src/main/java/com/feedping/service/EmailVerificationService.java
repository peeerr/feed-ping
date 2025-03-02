package com.feedping.service;

import com.feedping.domain.Member;
import com.feedping.repository.MemberRepository;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class EmailVerificationService {

    private final MemberRepository memberRepository;
    private final VerificationCodeStore codeManager;
    private final EmailSenderService emailSenderService;
    private final AuthTokenService authTokenService;

    public void sendVerificationEmail(String email) {
        String code = codeManager.createCode(email);

        CompletableFuture<Boolean> future = emailSenderService.sendVerificationEmail(email, code);

        // 오류 처리를 위한 콜백 등록
        future.whenComplete((success, ex) -> {
            if (ex != null) {
                log.error("인증 이메일 전송에 실패했습니다. 수신자: {}", email, ex);
            } else if (!success) {
                // 전송은 성공했으나 이메일 서버에서 처리 실패
                log.error("인증 이메일 전송에 실패했습니다. 수신자: {} (반환값: false)", email);
            }
        });
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
