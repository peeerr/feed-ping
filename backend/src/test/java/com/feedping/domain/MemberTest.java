package com.feedping.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberTest {

    @Test
    @DisplayName("Member 객체가 성공적으로 생성된다")
    void should_CreateMember_When_ValidInput() {
        // given
        Long id = 1L;
        String email = "test@example.com";

        // when
        Member member = Member.builder()
                .id(id)
                .email(email)
                .build();

        // then
        assertThat(member).isNotNull();
        assertThat(member.getId()).isEqualTo(id);
        assertThat(member.getEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("기본 생성자로 생성된 Member 객체는 기본값을 가진다")
    void should_HaveDefaultValues_When_CreatedWithDefaultConstructor() {
        // when
        Member member = new Member();

        // then
        assertThat(member).isNotNull();
        assertThat(member.getId()).isNull();
        assertThat(member.getEmail()).isNull();
    }

}
