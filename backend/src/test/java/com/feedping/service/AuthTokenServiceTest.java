package com.feedping.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.feedping.domain.AuthToken;
import com.feedping.domain.Member;
import com.feedping.exception.ErrorCode;
import com.feedping.exception.GlobalException;
import com.feedping.repository.AuthTokenRepository;
import com.feedping.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceTest {

    @InjectMocks
    private AuthTokenService authTokenService;

    @Mock
    private AuthTokenRepository authTokenRepository;

    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("이메일로 토큰을 생성할 때 해당 회원이 존재하지 않으면 예외가 발생한다")
    void should_ThrowException_When_MemberNotFoundForEmail() {
        // given
        String email = "nonexistent@example.com";
        given(memberRepository.findByEmail(email)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authTokenService.createTokenForEmail(email))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND_MEMBER);

        then(memberRepository).should().findByEmail(email);
        then(authTokenRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("이메일로 토큰을 생성할 때 이미 토큰이 있으면 기존 토큰을 반환한다")
    void should_ReturnExistingToken_When_TokenAlreadyExistsForMember() {
        // given
        String email = "test@example.com";
        String existingToken = "existing-token";

        Member member = Member.builder()
                .id(1L)
                .email(email)
                .build();

        AuthToken authToken = AuthToken.builder()
                .id(1L)
                .token(existingToken)
                .member(member)
                .build();

        given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));
        given(authTokenRepository.findByMember(member)).willReturn(Optional.of(authToken));

        // when
        String result = authTokenService.createTokenForEmail(email);

        // then
        assertThat(result).isEqualTo(existingToken);

        then(memberRepository).should().findByEmail(email);
        then(authTokenRepository).should().findByMember(member);
        then(authTokenRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("이메일로 토큰을 생성할 때 토큰이 없으면 새 토큰을 생성한다")
    void should_CreateNewToken_When_NoTokenExistsForMember() {
        // given
        String email = "test@example.com";

        Member member = Member.builder()
                .id(1L)
                .email(email)
                .build();

        given(memberRepository.findByEmail(email)).willReturn(Optional.of(member));
        given(authTokenRepository.findByMember(member)).willReturn(Optional.empty());
        given(authTokenRepository.save(any(AuthToken.class))).willAnswer(invocation -> {
            AuthToken savedToken = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedToken, "id", 1L);
            return savedToken;
        });

        // when
        String result = authTokenService.createTokenForEmail(email);

        // then
        assertThat(result).isNotNull().isNotEmpty();

        then(memberRepository).should().findByEmail(email);
        then(authTokenRepository).should().findByMember(member);
        then(authTokenRepository).should().save(any(AuthToken.class));
    }

    @Test
    @DisplayName("토큰으로 회원을 조회할 때 토큰이 유효하면 회원을 반환한다")
    void should_ReturnMember_When_TokenIsValid() {
        // given
        String token = "valid-token";
        Member member = Member.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        AuthToken authToken = AuthToken.builder()
                .id(1L)
                .token(token)
                .member(member)
                .build();

        given(authTokenRepository.findByToken(token)).willReturn(Optional.of(authToken));

        // when
        Member result = authTokenService.validateAndGetMember(token);

        // then
        assertThat(result).isEqualTo(member);

        then(authTokenRepository).should().findByToken(token);
    }

    @Test
    @DisplayName("토큰으로 회원을 조회할 때 토큰이 유효하지 않으면 예외가 발생한다")
    void should_ThrowException_When_TokenIsInvalid() {
        // given
        String token = "invalid-token";
        given(authTokenRepository.findByToken(token)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authTokenService.validateAndGetMember(token))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);

        then(authTokenRepository).should().findByToken(token);
    }

    @Test
    @DisplayName("회원에 대한 토큰을 생성할 때 이미 토큰이 있으면 아무 작업도 하지 않는다")
    void should_DoNothing_When_MemberAlreadyHasToken() {
        // given
        Member member = Member.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        given(authTokenRepository.existsByMember(member)).willReturn(true);

        // when
        authTokenService.createTokenForMember(member);

        // then
        then(authTokenRepository).should().existsByMember(member);
        then(authTokenRepository).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("회원에 대한 토큰을 생성할 때 토큰이 없으면 새 토큰을 생성한다")
    void should_CreateNewToken_When_MemberHasNoToken() {
        // given
        Member member = Member.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        given(authTokenRepository.existsByMember(member)).willReturn(false);
        given(authTokenRepository.save(any(AuthToken.class))).willAnswer(invocation -> {
            AuthToken savedToken = invocation.getArgument(0);
            ReflectionTestUtils.setField(savedToken, "id", 1L);
            return savedToken;
        });

        // when
        authTokenService.createTokenForMember(member);

        // then
        then(authTokenRepository).should().existsByMember(member);
        then(authTokenRepository).should().save(any(AuthToken.class));
    }

}
