package com.feedping.notification;

import com.feedping.metrics.NotificationMetrics;
import com.feedping.repository.SubscriptionRepository;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PriorityNotificationQueue {

    // 우선순위별 알림 큐 관리
    private final Map<Priority, LinkedBlockingQueue<NotificationTask>> priorityQueues;

    // 피드별 구독자 수 캐시
    private final Map<Long, Integer> feedSubscriberCountCache;

    private final SubscriptionRepository subscriberRepository;

    private final NotificationMetrics metrics;

    public enum Priority {
        HIGH(0),    // 구독자 100명 이상 (t2.micro 환경 고려)
        MEDIUM(1),  // 구독자 20-99명
        LOW(2);     // 구독자 20명 미만

        private final int value;

        Priority(int value) {
            this.value = value;
        }
    }

    public PriorityNotificationQueue(SubscriptionRepository subscriberRepository, NotificationMetrics metrics) {
        this.subscriberRepository = subscriberRepository;
        this.metrics = metrics;
        priorityQueues = new EnumMap<>(Priority.class);
        for (Priority priority : Priority.values()) {
            priorityQueues.put(priority, new LinkedBlockingQueue<>());
        }

        // 초기 구독자 수 로드
        feedSubscriberCountCache = new ConcurrentHashMap<>();
        initFeedSubscriberCounts();
    }

    private void initFeedSubscriberCounts() {
        try {
            // 모든 피드의 구독자 수 로드
            subscriberRepository.findFeedSubscriberCounts().forEach(
                    feedCount -> feedSubscriberCountCache.put(
                            feedCount.getRssFeedId(),
                            feedCount.getSubscriberCountAsInt()
                    )
            );

            log.info("피드 구독자 수 초기화 완료: {} 개의 피드", feedSubscriberCountCache.size());
        } catch (Exception e) {
            log.error("피드 구독자 수 초기화 실패", e);
        }
    }

    public void addNotification(NotificationTask task) {
        Priority priority = calculatePriority(task.getRssFeedId());
        priorityQueues.get(priority).add(task);
        updateMetrics();
        log.debug("{} 우선순위 큐에 알림 추가: 피드={}, 수신자={}",
                priority, task.getRssFeedId(), task.getEmail());
    }

    public List<NotificationTask> drainTasks(int maxBatchSize) {
        List<NotificationTask> batch = new ArrayList<>(maxBatchSize);

        // 우선순위 높은 큐부터 차례로 처리
        for (Priority priority : Priority.values()) {
            if (batch.size() >= maxBatchSize) {
                break;
            }

            LinkedBlockingQueue<NotificationTask> queue = priorityQueues.get(priority);
            queue.drainTo(batch, maxBatchSize - batch.size());
        }

        updateMetrics();
        log.debug("우선순위 큐에서 {} 개의 작업 추출", batch.size());
        return batch;
    }

    public int getTotalSize() {
        return priorityQueues.values().stream()
                .mapToInt(LinkedBlockingQueue::size)
                .sum();
    }

    public Map<Priority, Integer> getQueueSizes() {
        Map<Priority, Integer> sizes = new EnumMap<>(Priority.class);
        for (Priority priority : Priority.values()) {
            sizes.put(priority, priorityQueues.get(priority).size());
        }
        return sizes;
    }

    private Priority calculatePriority(Long rssFeedId) {
        int subscriberCount = feedSubscriberCountCache.getOrDefault(rssFeedId, 0);

        if (subscriberCount >= 100) {  // t2.micro 환경에 맞게 임계값 조정
            return Priority.HIGH;
        } else if (subscriberCount >= 20) {
            return Priority.MEDIUM;
        } else {
            return Priority.LOW;
        }
    }

    // 구독자 수 캐시 업데이트 메서드
    public void updateSubscriberCount(Long rssFeedId, int count) {
        feedSubscriberCountCache.put(rssFeedId, count);
    }

    // 메트릭 업데이트 메서드
    public void updateMetrics() {
        Map<String, Integer> sizes = new HashMap<>();
        sizes.put("high", priorityQueues.get(Priority.HIGH).size());
        sizes.put("medium", priorityQueues.get(Priority.MEDIUM).size());
        sizes.put("low", priorityQueues.get(Priority.LOW).size());

        metrics.updatePriorityQueueSizes(sizes);
    }

}
