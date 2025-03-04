package com.feedping.repository;

import com.feedping.domain.Member;
import com.feedping.domain.RssFeed;
import com.feedping.domain.Subscription;
import com.feedping.dto.FeedSubscriberCount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByMemberAndRssFeed(Member member, RssFeed rssFeed);

    Page<Subscription> findByMember(Member member, Pageable pageable);

    Optional<Subscription> findByMemberAndRssFeed(Member member, RssFeed rssFeed);

    @EntityGraph(attributePaths = {"member"})
    List<Subscription> findByRssFeed(RssFeed rssFeed);

    @Query("SELECT new com.feedping.dto.FeedSubscriberCount(s.rssFeed.id, COUNT(s.id)) " +
            "FROM Subscription s GROUP BY s.rssFeed.id")
    List<FeedSubscriberCount> findFeedSubscriberCounts();

}
