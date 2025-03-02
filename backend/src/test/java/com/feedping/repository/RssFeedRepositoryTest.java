package com.feedping.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.feedping.domain.RssFeed;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class RssFeedRepositoryTest {

    @Autowired
    private RssFeedRepository rssFeedRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("RSS 피드를 저장하고 ID로 조회한다")
    void should_SaveAndFindById_When_ValidRssFeed() {
        // given
        RssFeed rssFeed = RssFeed.builder()
                .url("https://example.com/rss.xml")
                .build();

        // when
        RssFeed savedRssFeed = rssFeedRepository.save(rssFeed);
        RssFeed foundRssFeed = rssFeedRepository.findById(savedRssFeed.getId()).orElse(null);

        // then
        assertThat(foundRssFeed).isNotNull();
        assertThat(foundRssFeed.getId()).isEqualTo(savedRssFeed.getId());
        assertThat(foundRssFeed.getUrl()).isEqualTo("https://example.com/rss.xml");
    }

    @Test
    @DisplayName("URL로 RSS 피드를 조회한다")
    void should_FindByUrl_When_RssFeedExists() {
        // given
        String url = "https://example.com/rss.xml";

        RssFeed rssFeed = RssFeed.builder()
                .url(url)
                .build();

        entityManager.persist(rssFeed);
        entityManager.flush();

        // when
        Optional<RssFeed> foundRssFeed = rssFeedRepository.findByUrl(url);

        // then
        assertThat(foundRssFeed).isPresent();
        assertThat(foundRssFeed.get().getUrl()).isEqualTo(url);
    }

    @Test
    @DisplayName("존재하지 않는 URL로 조회하면 빈 Optional을 반환한다")
    void should_ReturnEmptyOptional_When_RssFeedNotExists() {
        // given
        String nonExistentUrl = "https://nonexistent.com/rss.xml";

        // when
        Optional<RssFeed> foundRssFeed = rssFeedRepository.findByUrl(nonExistentUrl);

        // then
        assertThat(foundRssFeed).isEmpty();
    }

}
