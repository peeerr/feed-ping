package com.feedping.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.feedping.domain.Member;
import com.feedping.domain.RssFeed;
import com.feedping.domain.Subscription;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@DataJpaTest
class SubscriptionRepositoryTest {

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("구독 정보를 저장하고 ID로 조회한다")
    void should_SaveAndFindById_When_ValidSubscription() {
        // given
        Member member = createAndPersistMember("test@example.com");
        RssFeed rssFeed = createAndPersistRssFeed("https://example.com/rss.xml");

        Subscription subscription = Subscription.builder()
                .member(member)
                .rssFeed(rssFeed)
                .siteName("Test Blog")
                .build();

        // when
        Subscription savedSubscription = subscriptionRepository.save(subscription);
        Subscription foundSubscription = subscriptionRepository.findById(savedSubscription.getId()).orElse(null);

        // then
        assertThat(foundSubscription).isNotNull();
        assertThat(foundSubscription.getId()).isEqualTo(savedSubscription.getId());
        assertThat(foundSubscription.getMember().getId()).isEqualTo(member.getId());
        assertThat(foundSubscription.getRssFeed().getId()).isEqualTo(rssFeed.getId());
        assertThat(foundSubscription.getSiteName()).isEqualTo("Test Blog");
        assertThat(foundSubscription.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("회원과 RSS 피드로 구독 존재 여부를 확인한다")
    void should_CheckExistsByMemberAndRssFeed() {
        // given
        Member member = createAndPersistMember("test@example.com");
        RssFeed rssFeed = createAndPersistRssFeed("https://example.com/rss.xml");

        Subscription subscription = Subscription.builder()
                .member(member)
                .rssFeed(rssFeed)
                .siteName("Test Blog")
                .build();

        entityManager.persist(subscription);
        entityManager.flush();

        // when
        boolean exists = subscriptionRepository.existsByMemberAndRssFeed(member, rssFeed);
        boolean notExists = subscriptionRepository.existsByMemberAndRssFeed(member,
                createAndPersistRssFeed("https://other.com/rss.xml"));

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("회원 ID로 구독 목록을 페이징하여 조회한다")
    void should_FindByMemberWithPaging_When_MemberHasSubscriptions() {
        // given
        Member member = createAndPersistMember("test@example.com");

        for (int i = 1; i <= 15; i++) {
            RssFeed rssFeed = createAndPersistRssFeed("https://example" + i + ".com/rss.xml");
            Subscription subscription = Subscription.builder()
                    .member(member)
                    .rssFeed(rssFeed)
                    .siteName("Test Blog " + i)
                    .build();
            entityManager.persist(subscription);
        }

        entityManager.flush();

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        // when
        Page<Subscription> subscriptions = subscriptionRepository.findByMember(member, pageable);

        // then
        assertThat(subscriptions.getTotalElements()).isEqualTo(15);
        assertThat(subscriptions.getContent()).hasSize(10);
        assertThat(subscriptions.getTotalPages()).isEqualTo(2);
        assertThat(subscriptions.getContent().get(0).getSiteName()).startsWith("Test Blog ");
    }

    @Test
    @DisplayName("회원과 RSS 피드로 구독 정보를 조회한다")
    void should_FindByMemberAndRssFeed_When_SubscriptionExists() {
        // given
        Member member = createAndPersistMember("test@example.com");
        RssFeed rssFeed = createAndPersistRssFeed("https://example.com/rss.xml");

        Subscription subscription = Subscription.builder()
                .member(member)
                .rssFeed(rssFeed)
                .siteName("Test Blog")
                .build();

        entityManager.persist(subscription);
        entityManager.flush();

        // when
        Optional<Subscription> foundSubscription = subscriptionRepository.findByMemberAndRssFeed(member, rssFeed);

        // then
        assertThat(foundSubscription).isPresent();
        assertThat(foundSubscription.get().getMember().getId()).isEqualTo(member.getId());
        assertThat(foundSubscription.get().getRssFeed().getId()).isEqualTo(rssFeed.getId());
        assertThat(foundSubscription.get().getSiteName()).isEqualTo("Test Blog");
    }

    @Test
    @DisplayName("RSS 피드로 구독 목록을 조회한다")
    void should_FindByRssFeed_When_SubscriptionsExist() {
        // given
        RssFeed rssFeed = createAndPersistRssFeed("https://example.com/rss.xml");

        for (int i = 1; i <= 3; i++) {
            Member member = createAndPersistMember("test" + i + "@example.com");
            Subscription subscription = Subscription.builder()
                    .member(member)
                    .rssFeed(rssFeed)
                    .siteName("Test Blog " + i)
                    .build();
            entityManager.persist(subscription);
        }

        entityManager.flush();

        // when
        List<Subscription> subscriptions = subscriptionRepository.findByRssFeed(rssFeed);

        // then
        assertThat(subscriptions).hasSize(3);
        assertThat(subscriptions.get(0).getRssFeed().getId()).isEqualTo(rssFeed.getId());
        assertThat(subscriptions.get(1).getRssFeed().getId()).isEqualTo(rssFeed.getId());
        assertThat(subscriptions.get(2).getRssFeed().getId()).isEqualTo(rssFeed.getId());
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

}
