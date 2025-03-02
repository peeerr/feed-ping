package com.feedping.service;

import static org.mockito.BDDMockito.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;

import com.feedping.domain.Member;
import com.feedping.repository.MemberRepository;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @InjectMocks
    private EmailVerificationService emailVerificationServiceTest;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private VerificationCodeStore codeManager;

    @Mock
    private EmailSenderService emailSenderService;

    @Mock
    private AuthTokenService authTokenService;

    @Test
    @DisplayName("인증 이메일이 성공적으로 전송된다")
    void should_SendVerificationEmail_Successfully() {
        // given
        String email = "test@example.com";
        String verificationCode = "123456";

        given(codeManager.createCode(email)).willReturn(verificationCode);
        given(emailSenderService.sendVerificationEmail(email, verificationCode))
                .willReturn(CompletableFuture.completedFuture(true));

        // when
        emailVerificationServiceTest.sendVerificationEmail(email);

        // then
        then(codeManager).should().createCode(email);
        then(emailSenderService).should().sendVerificationEmail(email, verificationCode);
    }

    @Test
    @DisplayName("기존 회원이 없을 때 이메일 인증 시 새 회원이 생성된다")
    void should_CreateNewMember_When_VerifyingEmailForNewUser() {
        // given
        String email = "test@example.com";
        String code = "123456";

        willDoNothing().given(codeManager).validateCode(email, code);
        given(memberRepository.findByEmail(email)).willReturn(Optional.empty());
        willDoNothing().given(authTokenService).createTokenForMember(any(Member.class));

        // when
        emailVerificationServiceTest.verifyEmail(email, code);

        // then
        then(codeManager).should().validateCode(email, code);
        then(memberRepository).should().findByEmail(email);
        then(memberRepository).should().save(any(Member.class));
        then(authTokenService).should().createTokenForMember(any(Member.class));
    }

    @Test
    @DisplayName("기존 회원이 있을 때 이메일 인증이 정상적으로 완료된다")
    void should_CompleteVerification_When_VerifyingEmailForExistingUser() {
        // given
        String email = "test@example.com";
        String code = "123456";
        Member existingMember = Member.builder()
                .id(1L)
                .email(email)
                .build();

        willDoNothing().given(codeManager).validateCode(email, code);
        given(memberRepository.findByEmail(email)).willReturn(Optional.of(existingMember));

        // when
        emailVerificationServiceTest.verifyEmail(email, code);

        // then
        then(codeManager).should().validateCode(email, code);
        then(memberRepository).should().findByEmail(email);
        then(memberRepository).should(never()).save(any(Member.class));
        then(authTokenService).should(never()).createTokenForMember(any(Member.class));
    }

}
