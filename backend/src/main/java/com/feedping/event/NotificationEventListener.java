package com.feedping.event;

import com.feedping.domain.Member;
import com.feedping.domain.RssFeed;
import com.feedping.metrics.NotificationMetrics;
import com.feedping.repository.SubscriptionRepository;
import com.feedping.service.EmailSenderService;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationEventListener {

    private final EmailSenderService emailSenderService;
    private final SubscriptionRepository subscriptionRepository;
    private final NotificationMetrics metrics;

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationEvent(RssNotificationEvent event) {
        Timer.Sample batchTimer = metrics.startTimer();

        int totalNotifications = event.getNotificationMap().values().stream()
                .mapToInt(list -> list.size())
                .sum();

        log.info("RSS 알림 이벤트 처리 시작: {} 명의 구독자, {} 개의 알림",
                event.getNotificationMap().size(), totalNotifications);

        // 알림 수 기록
        metrics.recordQueued(totalNotifications);
        metrics.incrementCurrentlyProcessing(totalNotifications);

        // 처리 상태 추적
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger failedCount = new AtomicInteger(0);

        event.getNotificationMap().forEach((member, items) -> {
            try {
                // 개별 알림 처리 타이머 시작
                Timer.Sample notificationTimer = metrics.startTimer();

                // 해당 구독자의 피드에 대한 사이트 이름 확인
                RssFeed rssFeed = items.get(0).getRssFeed();
                String siteName = getSiteName(member, rssFeed, event.getSiteName());

                try {
                    // 비동기 이메일 발송 시도
                    CompletableFuture<Boolean> future = emailSenderService.sendRssNotification(
                            member.getEmail(),
                            siteName,
                            items
                    );

                    future.whenComplete((success, ex) -> {
                        // 처리 완료 시 타이머 종료
                        metrics.stopProcessingTimer(notificationTimer);

                        if (ex != null) {
                            log.error("알림 이메일 전송에 실패했습니다. 수신자: {}", member.getEmail(), ex);
                            failedCount.addAndGet(items.size());
                            metrics.recordFailed(items.size());
                            metrics.recordEmailFailedByReason(ex.getClass().getSimpleName());
                        } else if (!success) {
                            log.error("알림 이메일 전송 실패: {} (반환값: false)", member.getEmail());
                            failedCount.addAndGet(items.size());
                            metrics.recordFailed(items.size());
                            metrics.recordEmailFailedByReason("UnknownFailure");
                        } else {
                            log.info("알림 이메일 전송 성공: {}, 아이템: {}", member.getEmail(), items.size());
                            processedCount.addAndGet(items.size());
                            metrics.recordProcessed(items.size());
                        }

                        // 진행 중인 작업 카운터 감소
                        metrics.decrementCurrentlyProcessing(items.size());

                        // 모든 작업이 완료되면 배치 타이머 종료
                        if (processedCount.get() + failedCount.get() >= totalNotifications) {
                            metrics.stopProcessingTimer(batchTimer);
                            log.info("알림 배치 처리 완료 - 성공: {}, 실패: {}",
                                    processedCount.get(), failedCount.get());
                        }
                    });
                } catch (TaskRejectedException e) {
                    // 작업 거부 오류 명시적 처리
                    log.error("작업 큐 포화로 작업이 거부되었습니다. 수신자: {}", member.getEmail(), e);
                    failedCount.addAndGet(items.size());
                    metrics.recordFailed(items.size());
                    metrics.recordTaskRejected(); // 작업 거부 카운터 증가
                    metrics.decrementCurrentlyProcessing(items.size());
                }
            } catch (Exception e) {
                log.error("알림 준비에 실패했습니다. 수신자: {}", member.getEmail(), e);
                failedCount.addAndGet(items.size());
                metrics.recordFailed(items.size());

                // 예외 유형 기록
                if (e instanceof TaskRejectedException) {
                    metrics.recordTaskRejected();
                } else {
                    metrics.recordEmailFailedByReason("PreparationFailure");
                }

                // 진행 중인 작업 카운터 감소
                metrics.decrementCurrentlyProcessing(items.size());
            }
        });
    }

    private String getSiteName(Member member, RssFeed rssFeed, String defaultName) {
        return subscriptionRepository.findByMemberAndRssFeed(member, rssFeed)
                .map(subscription -> subscription.getSiteName())
                .orElse(defaultName);
    }

}
