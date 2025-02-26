package com.feedping.service;

import com.feedping.domain.Member;
import com.feedping.domain.MemberReceivedItem;
import com.feedping.domain.RssFeed;
import com.feedping.domain.RssItem;
import com.feedping.domain.Subscription;
import com.feedping.dto.RssItemDto;
import com.feedping.event.RssNotificationEvent;
import com.feedping.exception.ErrorCode;
import com.feedping.exception.GlobalException;
import com.feedping.repository.MemberReceivedItemRepository;
import com.feedping.repository.RssFeedRepository;
import com.feedping.repository.RssItemRepository;
import com.feedping.repository.SubscriptionRepository;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import java.io.StringReader;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@RequiredArgsConstructor
@Service
public class RssFeedSyncService {

    private final RssFeedRepository rssFeedRepository;
    private final RssItemRepository rssItemRepository;
    private final MemberReceivedItemRepository memberReceivedItemRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final RestTemplate restTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void syncAllFeeds() {
        List<RssFeed> rssFeeds = rssFeedRepository.findAll();

        for (RssFeed rssFeed : rssFeeds) {
            try {
                syncFeed(rssFeed);
            } catch (Exception e) {
                log.error("RSS 피드 동기화에 실패했습니다. URL: {}", rssFeed.getUrl(), e);
            }
        }
    }

    public void syncFeed(RssFeed rssFeed) {
        List<RssItemDto> fetchedEntries = fetchAndParseRssFeed(rssFeed.getUrl());

        if (!fetchedEntries.isEmpty()) {
            processNewItems(rssFeed, fetchedEntries);
        }
    }

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

    private List<RssItemDto> fetchAndParseRssFeed(String url) {
        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(url)
                    .build(true)  // 이미 인코딩된 상태를 유지
                    .toUri();

            // 여기서 대용량 데이터 문제가 발생할 수 있음
            // 향후 개선 필요: 스트림 방식 파싱 또는 응답 크기 제한 구현
            String feedContent = restTemplate.getForObject(uri, String.class);

            try (StringReader reader = new StringReader(feedContent)) {
                SyndFeed feed = new SyndFeedInput().build(reader);

                return feed.getEntries().stream()
                        .map(RssItemDto::from)
                        .filter(this::isValidRssItem)
                        .limit(20)  // 최대 20개 항목으로 제한
                        .toList();
            }
        } catch (Exception e) {
            log.error("RSS 피드 가져오기 또는 파싱에 실패했습니다. URL: {}", url, e);
            throw new GlobalException(ErrorCode.RSS_FEED_PARSING_ERROR);
        }
    }

    private boolean isValidRssItem(RssItemDto item) {
        return StringUtils.hasText(item.getTitle()) &&
                StringUtils.hasText(item.getLink());
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
