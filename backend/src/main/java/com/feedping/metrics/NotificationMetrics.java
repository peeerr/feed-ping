package com.feedping.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * RSS 알림 처리 관련 메트릭스 수집
 */
@Component
public class NotificationMetrics {

    private final MeterRegistry registry;

    private final Counter notificationsQueuedCounter;
    private final Counter notificationsProcessedCounter;
    private final Counter notificationsFailedCounter;
    private final Counter emailsSentCounter;
    private final Counter emailsFailedCounter;
    private final Counter feedsProcessedCounter;

    private final Timer notificationProcessingTimer;
    private final Timer emailSendingTimer;
    private final Timer feedProcessingTimer;

    private final AtomicInteger currentlyProcessingNotifications;
    private final AtomicInteger emailQueueSize;

    // 이메일 실패 이유별 카운터
    private final ConcurrentHashMap<String, Counter> emailFailuresByReason;

    public NotificationMetrics(MeterRegistry registry) {
        this.registry = registry;
        
        // 알림 처리량 관련 카운터
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

        // 알림 처리 시간 측정 타이머
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

        // 현재 상태 모니터링을 위한 게이지
        this.currentlyProcessingNotifications = new AtomicInteger(0);
        Gauge.builder("feedping.notifications.currently_processing", currentlyProcessingNotifications, AtomicInteger::get)
                .description("현재 처리 중인 알림 건수")
                .register(registry);

        this.emailQueueSize = new AtomicInteger(0);
        Gauge.builder("feedping.emails.queue_size", emailQueueSize, AtomicInteger::get)
                .description("이메일 전송 큐의 현재 크기")
                .register(registry);

        // 이메일 실패 이유별 카운터를 위한 맵
        this.emailFailuresByReason = new ConcurrentHashMap<>();
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

    // 타이머 샘플 시작 (더 정확한 측정을 위해)
    public Timer.Sample startTimer() {
        return Timer.start();
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

}
