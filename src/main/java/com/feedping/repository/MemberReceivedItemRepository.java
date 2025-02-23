package com.feedping.repository;

import com.feedping.domain.Member;
import com.feedping.domain.MemberReceivedItem;
import com.feedping.domain.RssItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberReceivedItemRepository extends JpaRepository<MemberReceivedItem, Long> {

    boolean existsByMemberAndRssItem(Member member, RssItem rssItem);

}
