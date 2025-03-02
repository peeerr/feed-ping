package com.feedping.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.feedping.domain.Member;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("회원을 저장하고 ID로 조회한다")
    void should_SaveAndFindById_When_ValidMember() {
        // given
        Member member = Member.builder()
                .email("test@example.com")
                .build();

        // when
        Member savedMember = memberRepository.save(member);
        Member foundMember = memberRepository.findById(savedMember.getId()).orElse(null);

        // then
        assertThat(foundMember).isNotNull();
        assertThat(foundMember.getId()).isEqualTo(savedMember.getId());
        assertThat(foundMember.getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("이메일로 회원을 조회한다")
    void should_FindByEmail_When_MemberExists() {
        // given
        String email = "test@example.com";

        Member member = Member.builder()
                .email(email)
                .build();

        entityManager.persist(member);
        entityManager.flush();

        // when
        Optional<Member> foundMember = memberRepository.findByEmail(email);

        // then
        assertThat(foundMember).isPresent();
        assertThat(foundMember.get().getEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회하면 빈 Optional을 반환한다")
    void should_ReturnEmptyOptional_When_MemberNotExists() {
        // given
        String nonExistentEmail = "nonexistent@example.com";

        // when
        Optional<Member> foundMember = memberRepository.findByEmail(nonExistentEmail);

        // then
        assertThat(foundMember).isEmpty();
    }

}
