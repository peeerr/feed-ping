package com.feedping.service;

import com.feedping.domain.Member;
import com.feedping.domain.MemberReceivedItem;
import com.feedping.domain.RssFeed;
import com.feedping.domain.RssItem;
import com.feedping.domain.Subscription;
import com.feedping.dto.RssItemDto;
import com.feedping.event.RssNotificationEvent;
import com.feedping.repository.MemberReceivedItemRepository;
import com.feedping.repository.RssItemRepository;
import com.feedping.repository.SubscriptionRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class RssItemProcessService {

    private final RssItemRepository rssItemRepository;
    private final MemberReceivedItemRepository memberReceivedItemRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    protected void processNewItems(RssFeed rssFeed, List<RssItemDto> entries) {
        List<RssItem> items = entries.stream()
                .map(entry -> rssItemRepository.findByLink(entry.getLink())
                        .orElseGet(() -> rssItemRepository.save(
                                RssItem.builder()
                                        .rssFeed(rssFeed)
                                        .link(entry.getLink())
                                        .title(entry.getTitle())
                                        .description(entry.getDescription())
                                        .publishedAt(entry.getPublishedAt())
                                        .build()
                        )))
                .toList();

        if (!items.isEmpty()) {
            List<Subscription> subscriptions = subscriptionRepository.findByRssFeed(rssFeed);
            Map<Member, List<RssItem>> notificationMap = createNotificationMap(subscriptions, items);

            // 알림이 필요한 구독자/아이템이 있으면 트랜잭션 이벤트 발행
            if (!notificationMap.isEmpty()) {
                // 사이트 이름은 첫 번째 구독에서 가져옴 (동일한 피드에 대해 구독자마다 다른 이름을 설정했을 수 있음)
                String siteName = subscriptions.isEmpty() ? "피드" :
                        subscriptions.get(0).getSiteName();

                eventPublisher.publishEvent(new RssNotificationEvent(notificationMap, siteName));
            }
        }
    }

    private Map<Member, List<RssItem>> createNotificationMap(List<Subscription> subscriptions, List<RssItem> newItems) {
        Map<Member, List<RssItem>> notificationMap = new HashMap<>();

        for (Subscription subscription : subscriptions) {
            Member subscriber = subscription.getMember();
            List<RssItem> unnotifiedItems = filterUnnotifiedItems(subscriber, newItems);

            if (!unnotifiedItems.isEmpty()) {
                notificationMap.put(subscriber, unnotifiedItems);
                markItemsAsNotified(subscriber, unnotifiedItems);
            }
        }

        return notificationMap;
    }

    private List<RssItem> filterUnnotifiedItems(Member subscriber, List<RssItem> items) {
        return items.stream()
                .filter(item -> !memberReceivedItemRepository
                        .existsByMemberAndRssItem(subscriber, item))
                .toList();
    }

    private void markItemsAsNotified(Member subscriber, List<RssItem> items) {
        items.forEach(item -> {
            MemberReceivedItem receivedItem = MemberReceivedItem.builder()
                    .member(subscriber)
                    .rssItem(item)
                    .build();
            receivedItem.markAsNotified();
            memberReceivedItemRepository.save(receivedItem);
        });
    }

}
