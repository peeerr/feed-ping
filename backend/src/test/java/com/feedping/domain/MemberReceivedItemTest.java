package com.feedping.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberReceivedItemTest {

    @Test
    @DisplayName("MemberReceivedItem 객체가 성공적으로 생성된다")
    void should_CreateMemberReceivedItem_When_ValidInput() {
        // given
        Member member = Member.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        RssFeed rssFeed = RssFeed.builder()
                .id(1L)
                .url("https://example.com/rss.xml")
                .build();

        RssItem rssItem = RssItem.builder()
                .id(1L)
                .rssFeed(rssFeed)
                .link("https://example.com/post/1")
                .title("Test Post")
                .description("This is a test post description")
                .publishedAt(LocalDateTime.now())
                .build();

        // when
        MemberReceivedItem receivedItem = MemberReceivedItem.builder()
                .member(member)
                .rssItem(rssItem)
                .build();

        // then
        assertThat(receivedItem).isNotNull();
        assertThat(receivedItem.getId()).isNull(); // ID는 자동 생성
        assertThat(receivedItem.getMember()).isEqualTo(member);
        assertThat(receivedItem.getRssItem()).isEqualTo(rssItem);
        assertThat(receivedItem.isNotified()).isFalse(); // 기본값은 false
    }

    @Test
    @DisplayName("기본 생성자로 생성된 MemberReceivedItem 객체는 기본값을 가진다")
    void should_HaveDefaultValues_When_CreatedWithDefaultConstructor() {
        // when
        MemberReceivedItem receivedItem = new MemberReceivedItem();

        // then
        assertThat(receivedItem).isNotNull();
        assertThat(receivedItem.getId()).isNull();
        assertThat(receivedItem.getMember()).isNull();
        assertThat(receivedItem.getRssItem()).isNull();
        assertThat(receivedItem.isNotified()).isFalse();
    }

    @Test
    @DisplayName("markAsNotified 메서드가 notified 플래그를 true로 설정한다")
    void should_SetNotifiedToTrue_When_MarkAsNotifiedCalled() {
        // given
        Member member = Member.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        RssFeed rssFeed = RssFeed.builder()
                .id(1L)
                .url("https://example.com/rss.xml")
                .build();

        RssItem rssItem = RssItem.builder()
                .id(1L)
                .rssFeed(rssFeed)
                .link("https://example.com/post/1")
                .title("Test Post")
                .description("This is a test post description")
                .publishedAt(LocalDateTime.now())
                .build();

        MemberReceivedItem receivedItem = MemberReceivedItem.builder()
                .member(member)
                .rssItem(rssItem)
                .build();

        assertThat(receivedItem.isNotified()).isFalse(); // 초기값 확인

        // when
        receivedItem.markAsNotified();

        // then
        assertThat(receivedItem.isNotified()).isTrue();
    }

}
