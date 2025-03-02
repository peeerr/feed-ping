package com.feedping.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.feedping.domain.Member;
import com.feedping.domain.MemberReceivedItem;
import com.feedping.domain.RssFeed;
import com.feedping.domain.RssItem;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class MemberReceivedItemRepositoryTest {

    @Autowired
    private MemberReceivedItemRepository memberReceivedItemRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("회원 수신 아이템을 저장하고 ID로 조회한다")
    void should_SaveAndFindById_When_ValidMemberReceivedItem() {
        // given
        Member member = createAndPersistMember("test@example.com");
        RssFeed rssFeed = createAndPersistRssFeed("https://example.com/rss.xml");
        RssItem rssItem = createAndPersistRssItem(rssFeed, "https://example.com/post/1");

        MemberReceivedItem receivedItem = MemberReceivedItem.builder()
                .member(member)
                .rssItem(rssItem)
                .build();

        // when
        MemberReceivedItem savedItem = memberReceivedItemRepository.save(receivedItem);
        MemberReceivedItem foundItem = memberReceivedItemRepository.findById(savedItem.getId()).orElse(null);

        // then
        assertThat(foundItem).isNotNull();
        assertThat(foundItem.getId()).isEqualTo(savedItem.getId());
        assertThat(foundItem.getMember().getId()).isEqualTo(member.getId());
        assertThat(foundItem.getRssItem().getId()).isEqualTo(rssItem.getId());
        assertThat(foundItem.isNotified()).isFalse();
    }

    @Test
    @DisplayName("회원과 RSS 아이템으로 수신 아이템 존재 여부를 확인한다")
    void should_CheckExistsByMemberAndRssItem() {
        // given
        Member member = createAndPersistMember("test@example.com");
        RssFeed rssFeed = createAndPersistRssFeed("https://example.com/rss.xml");
        RssItem rssItem = createAndPersistRssItem(rssFeed, "https://example.com/post/1");
        RssItem otherRssItem = createAndPersistRssItem(rssFeed, "https://example.com/post/2");

        MemberReceivedItem receivedItem = MemberReceivedItem.builder()
                .member(member)
                .rssItem(rssItem)
                .build();

        entityManager.persist(receivedItem);
        entityManager.flush();

        // when
        boolean exists = memberReceivedItemRepository.existsByMemberAndRssItem(member, rssItem);
        boolean notExists = memberReceivedItemRepository.existsByMemberAndRssItem(member, otherRssItem);

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("알림 표시를 설정하고 확인한다")
    void should_MarkAsNotified_And_CheckNotifiedStatus() {
        // given
        Member member = createAndPersistMember("test@example.com");
        RssFeed rssFeed = createAndPersistRssFeed("https://example.com/rss.xml");
        RssItem rssItem = createAndPersistRssItem(rssFeed, "https://example.com/post/1");

        MemberReceivedItem receivedItem = MemberReceivedItem.builder()
                .member(member)
                .rssItem(rssItem)
                .build();

        MemberReceivedItem savedItem = entityManager.persist(receivedItem);
        entityManager.flush();

        assertThat(savedItem.isNotified()).isFalse();

        // when
        savedItem.markAsNotified();
        entityManager.persist(savedItem);
        entityManager.flush();
        entityManager.clear();

        MemberReceivedItem foundItem = memberReceivedItemRepository.findById(savedItem.getId()).orElse(null);

        // then
        assertThat(foundItem).isNotNull();
        assertThat(foundItem.isNotified()).isTrue();
    }

    // 헬퍼 메서드
    private Member createAndPersistMember(String email) {
        Member member = Member.builder()
                .email(email)
                .build();
        return entityManager.persist(member);
    }

    private RssFeed createAndPersistRssFeed(String url) {
        RssFeed rssFeed = RssFeed.builder()
                .url(url)
                .build();
        return entityManager.persist(rssFeed);
    }

    private RssItem createAndPersistRssItem(RssFeed rssFeed, String link) {
        RssItem rssItem = RssItem.builder()
                .rssFeed(rssFeed)
                .link(link)
                .title("Test Post")
                .description("This is a test post description")
                .publishedAt(LocalDateTime.now())
                .build();
        return entityManager.persist(rssItem);
    }

}
