package com.feedping.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RssFeedTest {

    @Test
    @DisplayName("RssFeed 객체가 성공적으로 생성된다")
    void should_CreateRssFeed_When_ValidInput() {
        // given
        Long id = 1L;
        String url = "https://example.com/rss.xml";

        // when
        RssFeed rssFeed = RssFeed.builder()
                .id(id)
                .url(url)
                .build();

        // then
        assertThat(rssFeed).isNotNull();
        assertThat(rssFeed.getId()).isEqualTo(id);
        assertThat(rssFeed.getUrl()).isEqualTo(url);
    }

    @Test
    @DisplayName("기본 생성자로 생성된 RssFeed 객체는 기본값을 가진다")
    void should_HaveDefaultValues_When_CreatedWithDefaultConstructor() {
        // when
        RssFeed rssFeed = new RssFeed();

        // then
        assertThat(rssFeed).isNotNull();
        assertThat(rssFeed.getId()).isNull();
        assertThat(rssFeed.getUrl()).isNull();
    }

}
