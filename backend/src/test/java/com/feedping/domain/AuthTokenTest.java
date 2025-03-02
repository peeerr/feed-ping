package com.feedping.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthTokenTest {

    @Test
    @DisplayName("AuthToken 객체가 성공적으로 생성된다")
    void should_CreateAuthToken_When_ValidInput() {
        // given
        Long id = 1L;
        String token = "test-token-value";
        Member member = Member.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        // when
        AuthToken authToken = AuthToken.builder()
                .id(id)
                .token(token)
                .member(member)
                .build();

        // then
        assertThat(authToken).isNotNull();
        assertThat(authToken.getId()).isEqualTo(id);
        assertThat(authToken.getToken()).isEqualTo(token);
        assertThat(authToken.getMember()).isEqualTo(member);
    }

    @Test
    @DisplayName("기본 생성자로 생성된 AuthToken 객체는 기본값을 가진다")
    void should_HaveDefaultValues_When_CreatedWithDefaultConstructor() {
        // when
        AuthToken authToken = new AuthToken();

        // then
        assertThat(authToken).isNotNull();
        assertThat(authToken.getId()).isNull();
        assertThat(authToken.getToken()).isNull();
        assertThat(authToken.getMember()).isNull();
    }

}
