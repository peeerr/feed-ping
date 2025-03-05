package com.feedping.service;

import com.feedping.metrics.NotificationMetrics;
import com.feedping.notification.PriorityNotificationQueue;
import com.feedping.repository.SubscriptionRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
    private final NotificationMetrics metrics;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedRate = 3600000) // 1시간마다 실행
    public void updateFeedPriorities() {
        log.info("구독자 수 기반 피드 우선순위 업데이트 시작");
        Timer.Sample updateTimer = metrics.startTimer();

        try {
            // 모든 피드의 구독자 수 조회
            subscriptionRepository.findFeedSubscriberCounts().forEach(feedCount -> {
                Long rssFeedId = feedCount.getRssFeedId();
                int count = feedCount.getSubscriberCountAsInt();

                // 기존에 저장된 값과 비교하여 변경되었는지 확인
                int previousCount = notificationQueue.getSubscriberCount(rssFeedId);

                // 변경된 경우만 업데이트 메트릭 기록
                if (previousCount != count) {
                    recordPriorityChange(rssFeedId, previousCount, count);
                }

                // 큐와 메트릭에 구독자 수 업데이트
                notificationQueue.updateSubscriberCount(rssFeedId, count);

                log.debug("피드 {} 구독자 수 업데이트: {} 명", rssFeedId, count);
            });

            // 업데이트 시간 측정 종료
            metrics.stopFeedProcessingTimer(updateTimer);
            log.info("피드 우선순위 업데이트 완료");
        } catch (Exception e) {
            log.error("피드 우선순위 업데이트 중 오류 발생", e);
        }
    }

    // 우선순위 변경 이벤트 기록
    private void recordPriorityChange(Long feedId, int oldCount, int newCount) {
        String oldPriority = calculatePriorityName(oldCount);
        String newPriority = calculatePriorityName(newCount);

        if (!oldPriority.equals(newPriority)) {
            log.info("피드 {} 우선순위 변경: {} -> {}, 구독자: {} -> {}",
                    feedId, oldPriority, newPriority, oldCount, newCount);

            // 우선순위 변경 카운터 증가
            meterRegistry.counter("feedping.priority.changes",
                            "feed_id", String.valueOf(feedId),
                            "old_priority", oldPriority,
                            "new_priority", newPriority)
                    .increment();
        }
    }

    // 구독자 수에 따른 우선순위 이름 결정
    private String calculatePriorityName(int subscriberCount) {
        if (subscriberCount >= 100) {
            return "high";
        } else if (subscriberCount >= 20) {
            return "medium";
        } else {
            return "low";
        }
    }

}
