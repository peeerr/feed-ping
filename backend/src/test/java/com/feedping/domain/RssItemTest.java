package com.feedping.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RssItemTest {

    @Test
    @DisplayName("RssItem 객체가 성공적으로 생성된다")
    void should_CreateRssItem_When_ValidInput() {
        // given
        Long id = 1L;
        RssFeed rssFeed = RssFeed.builder()
                .id(1L)
                .url("https://example.com/rss.xml")
                .build();

        String link = "https://example.com/post/1";
        String title = "Test Post";
        String description = "This is a test post description";
        LocalDateTime publishedAt = LocalDateTime.now();

        // when
        RssItem rssItem = RssItem.builder()
                .id(id)
                .rssFeed(rssFeed)
                .link(link)
                .title(title)
                .description(description)
                .publishedAt(publishedAt)
                .build();

        // then
        assertThat(rssItem).isNotNull();
        assertThat(rssItem.getId()).isEqualTo(id);
        assertThat(rssItem.getRssFeed()).isEqualTo(rssFeed);
        assertThat(rssItem.getLink()).isEqualTo(link);
        assertThat(rssItem.getTitle()).isEqualTo(title);
        assertThat(rssItem.getDescription()).isEqualTo(description);
        assertThat(rssItem.getPublishedAt()).isEqualTo(publishedAt);
    }

    @Test
    @DisplayName("기본 생성자로 생성된 RssItem 객체는 기본값을 가진다")
    void should_HaveDefaultValues_When_CreatedWithDefaultConstructor() {
        // when
        RssItem rssItem = new RssItem();

        // then
        assertThat(rssItem).isNotNull();
        assertThat(rssItem.getId()).isNull();
        assertThat(rssItem.getRssFeed()).isNull();
        assertThat(rssItem.getLink()).isNull();
        assertThat(rssItem.getTitle()).isNull();
        assertThat(rssItem.getDescription()).isNull();
        assertThat(rssItem.getPublishedAt()).isNull();
    }

}
