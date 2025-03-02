package com.feedping.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SubscriptionTest {

    @Test
    @DisplayName("Subscription 객체가 성공적으로 생성된다")
    void should_CreateSubscription_When_ValidInput() {
        // given
        Long id = 1L;
        Member member = Member.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        RssFeed rssFeed = RssFeed.builder()
                .id(1L)
                .url("https://example.com/rss.xml")
                .build();

        String siteName = "Test Blog";

        // when
        Subscription subscription = Subscription.builder()
                .id(id)
                .member(member)
                .rssFeed(rssFeed)
                .siteName(siteName)
                .build();

        // then
        assertThat(subscription).isNotNull();
        assertThat(subscription.getId()).isEqualTo(id);
        assertThat(subscription.getMember()).isEqualTo(member);
        assertThat(subscription.getRssFeed()).isEqualTo(rssFeed);
        assertThat(subscription.getSiteName()).isEqualTo(siteName);
        assertThat(subscription.getCreatedAt()).isNull(); // createdAt은 JPA에 의해 자동 설정
    }

    @Test
    @DisplayName("기본 생성자로 생성된 Subscription 객체는 기본값을 가진다")
    void should_HaveDefaultValues_When_CreatedWithDefaultConstructor() {
        // when
        Subscription subscription = new Subscription();

        // then
        assertThat(subscription).isNotNull();
        assertThat(subscription.getId()).isNull();
        assertThat(subscription.getMember()).isNull();
        assertThat(subscription.getRssFeed()).isNull();
        assertThat(subscription.getSiteName()).isNull();
        assertThat(subscription.getCreatedAt()).isNull();
    }

}
