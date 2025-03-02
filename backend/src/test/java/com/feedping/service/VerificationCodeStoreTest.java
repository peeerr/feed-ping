package com.feedping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.never;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.atLeastOnce;

import com.feedping.exception.ErrorCode;
import com.feedping.exception.GlobalException;
import com.feedping.service.VerificationCodeStore;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class VerificationCodeStoreTest {

    @InjectMocks
    private VerificationCodeStore verificationCodeStore;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    @DisplayName("인증 코드가 성공적으로 생성된다")
    void should_CreateVerificationCode_Successfully() {
        // given
        String email = "test@example.com";

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        willDoNothing().given(valueOperations).set(anyString(), anyString(), any(Duration.class));

        // when
        String code = verificationCodeStore.createCode(email);

        // then
        assertThat(code).isNotNull().hasSize(6).matches("\\d{6}");

        then(redisTemplate).should().opsForValue();
        then(valueOperations).should().set(eq("email_verification:" + email), eq(code), any(Duration.class));
    }

    @Test
    @DisplayName("유효한 인증 코드 검증에 성공한다")
    void should_ValidateCode_When_CodeIsValid() {
        // given
        String email = "test@example.com";
        String code = "123456";
        String verificationKey = "email_verification:" + email;
        String verifiedKey = "verified_email:" + email;

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(verificationKey)).willReturn(code);
        given(redisTemplate.delete(verificationKey)).willReturn(true);
        willDoNothing().given(valueOperations).set(eq(verifiedKey), anyString(), any(Duration.class));

        // when
        verificationCodeStore.validateCode(email, code);

        // then
        then(redisTemplate).should(atLeastOnce()).opsForValue();
        then(valueOperations).should().get(verificationKey);
        then(redisTemplate).should().delete(verificationKey);
        then(valueOperations).should().set(eq(verifiedKey), eq("verified"), any(Duration.class));
    }

    @Test
    @DisplayName("유효하지 않은 인증 코드 검증에 실패한다")
    void should_ThrowException_When_CodeIsInvalid() {
        // given
        String email = "test@example.com";
        String validCode = "123456";
        String invalidCode = "654321";
        String verificationKey = "email_verification:" + email;

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(verificationKey)).willReturn(validCode);

        // when & then
        assertThatThrownBy(() -> verificationCodeStore.validateCode(email, invalidCode))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VERIFICATION_TOKEN);

        then(redisTemplate).should().opsForValue();
        then(valueOperations).should().get(verificationKey);
        then(redisTemplate).should(never()).delete(anyString());
        then(valueOperations).should(never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("저장된 인증 코드가 없을 때 검증에 실패한다")
    void should_ThrowException_When_NoStoredCode() {
        // given
        String email = "test@example.com";
        String code = "123456";
        String verificationKey = "email_verification:" + email;

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(verificationKey)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> verificationCodeStore.validateCode(email, code))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_VERIFICATION_TOKEN);

        then(redisTemplate).should().opsForValue();
        then(valueOperations).should().get(verificationKey);
        then(redisTemplate).should(never()).delete(anyString());
        then(valueOperations).should(never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("이메일이 인증된 상태인지 확인한다")
    void should_ReturnTrue_When_EmailIsVerified() {
        // given
        String email = "test@example.com";
        String verifiedKey = "verified_email:" + email;

        given(redisTemplate.hasKey(verifiedKey)).willReturn(true);

        // when
        boolean isVerified = verificationCodeStore.isEmailVerified(email);

        // then
        assertThat(isVerified).isTrue();
        then(redisTemplate).should().hasKey(verifiedKey);
    }

    @Test
    @DisplayName("이메일이 인증되지 않은 상태인지 확인한다")
    void should_ReturnFalse_When_EmailIsNotVerified() {
        // given
        String email = "test@example.com";
        String verifiedKey = "verified_email:" + email;

        given(redisTemplate.hasKey(verifiedKey)).willReturn(false);

        // when
        boolean isVerified = verificationCodeStore.isEmailVerified(email);

        // then
        assertThat(isVerified).isFalse();
        then(redisTemplate).should().hasKey(verifiedKey);
    }

    @Test
    @DisplayName("이메일 인증 정보가 성공적으로 취소된다")
    void should_RevokeVerification_Successfully() {
        // given
        String email = "test@example.com";
        String verificationKey = "email_verification:" + email;
        String verifiedKey = "verified_email:" + email;

        given(redisTemplate.delete(verificationKey)).willReturn(true);
        given(redisTemplate.delete(verifiedKey)).willReturn(true);

        // when
        verificationCodeStore.revokeVerification(email);

        // then
        then(redisTemplate).should().delete(verificationKey);
        then(redisTemplate).should().delete(verifiedKey);
    }

}
