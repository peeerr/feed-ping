package com.feedping.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class NotificationMetrics {

    private final MeterRegistry registry;

    // 기존 카운터
    private final Counter notificationsQueuedCounter;
    private final Counter notificationsProcessedCounter;
    private final Counter notificationsFailedCounter;
    private final Counter emailsSentCounter;
    private final Counter emailsFailedCounter;
    private final Counter feedsProcessedCounter;
    private final Counter taskRejectedCounter;

    // 새로운 우선순위 관련 게이지
    private final Map<String, AtomicInteger> priorityQueueSizes;
    private final AtomicInteger totalQueueSize;

    // 배치 처리 관련 메트릭
    private final Counter batchesProcessedCounter;
    private final Timer batchProcessingTimer;
    private final AtomicInteger avgBatchSize;

    // 재시도 관련 메트릭
    private final Counter retryAttemptsCounter;
    private final Counter retrySuccessCounter;

    // 타이머
    private final Timer notificationProcessingTimer;
    private final Timer emailSendingTimer;
    private final Timer feedProcessingTimer;

    // 상태 관련
    private final AtomicInteger currentlyProcessingNotifications;
    private final AtomicInteger emailQueueSize;

    // 실패 유형 카운터
    private final ConcurrentHashMap<String, Counter> emailFailuresByReason;

    public NotificationMetrics(MeterRegistry registry) {
        this.registry = registry;

        // 기본 카운터 초기화
        this.notificationsQueuedCounter = Counter.builder("feedping.notifications.queued")
                .description("알림 큐에 추가된 총 건수")
                .register(registry);

        this.notificationsProcessedCounter = Counter.builder("feedping.notifications.processed")
                .description("성공적으로 처리된 알림 건수")
                .register(registry);

        this.notificationsFailedCounter = Counter.builder("feedping.notifications.failed")
                .description("처리에 실패한 알림 건수")
                .register(registry);

        this.emailsSentCounter = Counter.builder("feedping.emails.sent")
                .description("전송된 이메일 총 건수")
                .register(registry);

        this.emailsFailedCounter = Counter.builder("feedping.emails.failed")
                .description("전송에 실패한 이메일 건수")
                .register(registry);

        this.feedsProcessedCounter = Counter.builder("feedping.feeds.processed")
                .description("처리된 RSS 피드 건수")
                .register(registry);

        this.taskRejectedCounter = Counter.builder("feedping.tasks.rejected")
                .description("큐 포화로 거부된 작업 건수")
                .register(registry);

        // 우선순위 관련 게이지 초기화
        this.priorityQueueSizes = new ConcurrentHashMap<>();
        this.priorityQueueSizes.put("high", new AtomicInteger(0));
        this.priorityQueueSizes.put("medium", new AtomicInteger(0));
        this.priorityQueueSizes.put("low", new AtomicInteger(0));

        // 우선순위별 큐 크기 게이지 등록
        priorityQueueSizes.forEach((priority, size) -> {
            Gauge.builder("feedping.priority_queue.size", size, AtomicInteger::get)
                    .tag("priority", priority)
                    .description("우선순위별 알림 큐 크기")
                    .register(registry);
        });

        // 전체 큐 크기
        this.totalQueueSize = new AtomicInteger(0);
        Gauge.builder("feedping.priority_queue.total_size", totalQueueSize, AtomicInteger::get)
                .description("전체 알림 큐 크기")
                .register(registry);

        // 배치 처리 관련 메트릭 초기화
        this.batchesProcessedCounter = Counter.builder("feedping.batches.processed")
                .description("처리된 알림 배치 수")
                .register(registry);

        this.batchProcessingTimer = Timer.builder("feedping.batches.processing.time")
                .description("배치 처리 소요 시간 (ms)")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);

        this.avgBatchSize = new AtomicInteger(0);
        Gauge.builder("feedping.batches.avg_size", avgBatchSize, AtomicInteger::get)
                .description("평균 배치 크기")
                .register(registry);

        // 재시도 관련 메트릭
        this.retryAttemptsCounter = Counter.builder("feedping.retries.attempts")
                .description("알림 재시도 시도 횟수")
                .register(registry);

        this.retrySuccessCounter = Counter.builder("feedping.retries.success")
                .description("알림 재시도 성공 횟수")
                .register(registry);

        // 타이머 초기화
        this.notificationProcessingTimer = Timer.builder("feedping.notifications.processing.time")
                .description("알림 처리 소요 시간 (ms)")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);

        this.emailSendingTimer = Timer.builder("feedping.emails.sending.time")
                .description("이메일 전송 소요 시간 (ms)")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);

        this.feedProcessingTimer = Timer.builder("feedping.feeds.processing.time")
                .description("RSS 피드 동기화 및 처리 소요 시간 (ms)")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(registry);

        // 상태 관련 게이지 초기화
        this.currentlyProcessingNotifications = new AtomicInteger(0);
        Gauge.builder("feedping.notifications.currently_processing", currentlyProcessingNotifications,
                        AtomicInteger::get)
                .description("현재 처리 중인 알림 건수")
                .register(registry);

        this.emailQueueSize = new AtomicInteger(0);
        Gauge.builder("feedping.emails.queue_size", emailQueueSize, AtomicInteger::get)
                .description("이메일 전송 큐의 현재 크기")
                .register(registry);

        // 실패 이유별 카운터 맵 초기화
        this.emailFailuresByReason = new ConcurrentHashMap<>();
    }

    // 우선순위 큐 크기 업데이트
    public void updatePriorityQueueSizes(Map<String, Integer> sizes) {
        int total = 0;

        for (Map.Entry<String, Integer> entry : sizes.entrySet()) {
            AtomicInteger gauge = priorityQueueSizes.get(entry.getKey().toLowerCase());
            if (gauge != null) {
                gauge.set(entry.getValue());
                total += entry.getValue();
            }
        }

        totalQueueSize.set(total);
    }

    // 배치 처리 메트릭
    public void recordBatchProcessed(int batchSize) {
        batchesProcessedCounter.increment();
        // 지수 이동 평균으로 평균 배치 크기 업데이트
        int currentAvg = avgBatchSize.get();
        if (currentAvg == 0) {
            avgBatchSize.set(batchSize);
        } else {
            // 80% 기존 값, 20% 새 값으로 지수 이동 평균 계산
            avgBatchSize.set((int) (currentAvg * 0.8 + batchSize * 0.2));
        }
    }

    public void recordBatchProcessingTime(long milliseconds) {
        batchProcessingTimer.record(Duration.ofMillis(milliseconds));
    }

    // 재시도 메트릭
    public void recordRetryAttempt() {
        retryAttemptsCounter.increment();
    }

    public void recordRetrySuccess() {
        retrySuccessCounter.increment();
    }

    // 큐에 추가된 알림 수 기록
    public void recordQueued(int count) {
        notificationsQueuedCounter.increment(count);
    }

    // 처리 완료된 알림 수 기록
    public void recordProcessed(int count) {
        notificationsProcessedCounter.increment(count);
    }

    // 실패한 알림 수 기록
    public void recordFailed(int count) {
        notificationsFailedCounter.increment(count);
    }

    // 이메일 성공 카운터
    public void recordEmailSent() {
        emailsSentCounter.increment();
    }

    // 이메일 실패 카운터
    public void recordEmailFailed() {
        emailsFailedCounter.increment();
    }

    // 작업 거부 카운터
    public void recordTaskRejected() {
        taskRejectedCounter.increment();
        recordEmailFailedByReason("TaskRejection");
    }

    // 실패 이유별 이메일 실패 추적
    public void recordEmailFailedByReason(String reason) {
        emailsFailedCounter.increment();
        emailFailuresByReason.computeIfAbsent(reason, r ->
                Counter.builder("feedping.emails.failed.reason")
                        .tag("reason", r)
                        .description("전송 실패한 이메일 (실패 이유별)")
                        .register(registry)
        ).increment();
    }

    // RSS 피드 처리 성공
    public void recordFeedProcessed() {
        feedsProcessedCounter.increment();
    }

    // RSS 피드 처리 실패
    public void recordFeedFailed() {
        Counter.builder("feedping.feeds.failed")
                .description("처리에 실패한 RSS 피드 건수")
                .register(registry)
                .increment();
    }

    // 알림 처리 시간 측정
    public void recordProcessingTime(long milliseconds) {
        notificationProcessingTimer.record(Duration.ofMillis(milliseconds));
    }

    // 이메일 전송 시간 측정
    public void recordEmailSendingTime(long milliseconds) {
        emailSendingTimer.record(Duration.ofMillis(milliseconds));
    }

    // RSS 피드 처리 시간 측정
    public void recordFeedProcessingTime(long milliseconds) {
        feedProcessingTimer.record(Duration.ofMillis(milliseconds));
    }

    // 현재 처리 중인 알림 수 증가
    public void incrementCurrentlyProcessing(int count) {
        currentlyProcessingNotifications.addAndGet(count);
    }

    // 현재 처리 중인 알림 수 감소
    public void decrementCurrentlyProcessing(int count) {
        currentlyProcessingNotifications.addAndGet(-count);
    }

    // 이메일 큐 크기 설정
    public void setEmailQueueSize(int size) {
        emailQueueSize.set(size);
    }

    // 현재 처리 중인 알림 수 조회
    public int getCurrentlyProcessingCount() {
        return currentlyProcessingNotifications.get();
    }

    // 타이머 샘플 시작
    public Timer.Sample startTimer() {
        return Timer.start(registry);
    }

    // 알림 처리 타이머 종료 및 기록
    public void stopProcessingTimer(Timer.Sample sample) {
        long timeNanos = sample.stop(notificationProcessingTimer);
        recordProcessingTime(timeNanos / 1_000_000);
    }

    // 이메일 전송 타이머 종료 및 기록
    public void stopEmailSendingTimer(Timer.Sample sample) {
        long timeNanos = sample.stop(emailSendingTimer);
        recordEmailSendingTime(timeNanos / 1_000_000);
    }

    // RSS 피드 처리 타이머 종료 및 기록
    public void stopFeedProcessingTimer(Timer.Sample sample) {
        long timeNanos = sample.stop(feedProcessingTimer);
        recordFeedProcessingTime(timeNanos / 1_000_000);
    }

    // 배치 처리 타이머 종료 및 기록
    public void stopBatchProcessingTimer(Timer.Sample sample) {
        long timeNanos = sample.stop(batchProcessingTimer);
        recordBatchProcessingTime(timeNanos / 1_000_000);
    }

}
