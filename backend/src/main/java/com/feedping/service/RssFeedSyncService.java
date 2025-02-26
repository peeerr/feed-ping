package com.feedping.service;


import com.feedping.domain.Member;
import com.feedping.domain.MemberReceivedItem;
import com.feedping.domain.RssFeed;
import com.feedping.domain.RssItem;
import com.feedping.domain.Subscription;
import com.feedping.dto.RssItemDto;
import com.feedping.exception.ErrorCode;
import com.feedping.exception.GlobalException;
import com.feedping.repository.MemberReceivedItemRepository;
import com.feedping.repository.RssFeedRepository;
import com.feedping.repository.RssItemRepository;
import com.feedping.repository.SubscriptionRepository;
import com.feedping.util.EmailSender;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import java.io.StringReader;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@RequiredArgsConstructor
@Transactional
@Service
public class RssFeedSyncService {

    private final RssFeedRepository rssFeedRepository;
    private final RssItemRepository rssItemRepository;
    private final MemberReceivedItemRepository memberReceivedItemRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final EmailSender emailSender;
    private final RestTemplate restTemplate;

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

        List<RssItem> itemsForNotification = fetchedEntries.stream()
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

        if (!itemsForNotification.isEmpty()) {
            processNewItems(rssFeed, itemsForNotification);
        }
    }

    protected void processNewItems(RssFeed rssFeed, List<RssItem> newItems) {
        List<Subscription> subscriptions = subscriptionRepository.findByRssFeed(rssFeed);
        Map<Member, List<RssItem>> notificationMap = createNotificationMap(subscriptions, newItems);

        sendNotificationsAsync(notificationMap);
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

    private void sendNotificationsAsync(Map<Member, List<RssItem>> notificationMap) {
        notificationMap.forEach((subscriber, items) -> {
            try {
                String siteName = getSubscriptionSiteName(subscriber, items.get(0).getRssFeed());

                CompletableFuture<Boolean> future = emailSender.sendRssNotification(
                        subscriber.getEmail(),
                        siteName,
                        items
                );

                future.whenComplete((success, ex) -> {
                    if (ex != null) {
                        log.error("알림 이메일 전송에 실패했습니다. 수신자: {}", subscriber.getEmail(), ex);
                    } else if (!success) {
                        log.error("알림 이메일 전송 실패: {} (반환값: false)", subscriber.getEmail());
                        // 필요 시 재시도 로직 추가 가능
                    }
                });
            } catch (Exception e) {
                log.error("알림 준비에 실패했습니다. 수신자: {}", subscriber.getEmail(), e);
            }
        });
    }

    private String getSubscriptionSiteName(Member subscriber, RssFeed rssFeed) {
        return subscriptionRepository.findByMemberAndRssFeed(subscriber, rssFeed)
                .orElseThrow(() -> new GlobalException(ErrorCode.SUBSCRIPTION_NOT_FOUND))
                .getSiteName();
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
