package com.feedping.service;

import com.feedping.notification.PriorityNotificationQueue;
import com.feedping.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class FeedPriorityUpdater {

    private final SubscriptionRepository subscriptionRepository;
    private final PriorityNotificationQueue notificationQueue;

    @Scheduled(fixedRate = 3600000) // 1시간마다 실행
    public void updateFeedPriorities() {
        log.info("구독자 수 기반 피드 우선순위 업데이트 시작");

        try {
            // 모든 피드의 구독자 수 조회
            subscriptionRepository.findFeedSubscriberCounts().forEach(feedCount -> {
                Long rssFeedId = feedCount.getRssFeedId();
                int count = feedCount.getSubscriberCountAsInt();

                notificationQueue.updateSubscriberCount(rssFeedId, count);
                log.debug("피드 {} 구독자 수 업데이트: {} 명", rssFeedId, count);
            });

            log.info("피드 우선순위 업데이트 완료");
        } catch (Exception e) {
            log.error("피드 우선순위 업데이트 중 오류 발생", e);
        }
    }

}
