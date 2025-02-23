package com.feedping.repository;

import com.feedping.domain.Member;
import com.feedping.domain.RssFeed;
import com.feedping.domain.Subscription;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    boolean existsByMemberAndRssFeed(Member member, RssFeed rssFeed);

    Page<Subscription> findByMember(Member member, Pageable pageable);

    Optional<Subscription> findByMemberAndRssFeed(Member member, RssFeed rssFeed);

}
