package com.feedping.event;

import com.feedping.domain.Member;
import com.feedping.domain.RssFeed;
import com.feedping.repository.SubscriptionRepository;
import com.feedping.service.EmailSenderService;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * RSS 알림 이벤트 리스너 트랜잭션 완료 후 이메일 발송을 담당
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class NotificationEventListener {

    private final EmailSenderService emailSenderService;
    private final SubscriptionRepository subscriptionRepository;

    /**
     * RSS 알림 이벤트 처리 트랜잭션이 성공적으로 커밋된 후에만 실행됩니다.
     */
    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationEvent(RssNotificationEvent event) {
        log.info("RSS 알림 이벤트 처리 시작: {} 명의 구독자", event.getNotificationMap().size());

        event.getNotificationMap().forEach((member, items) -> {
            try {
                // 해당 구독자의 피드에 대한 사이트 이름 확인
                RssFeed rssFeed = items.get(0).getRssFeed();
                String siteName = getSiteName(member, rssFeed, event.getSiteName());

                // 비동기 이메일 발송
                CompletableFuture<Boolean> future = emailSenderService.sendRssNotification(
                        member.getEmail(),
                        siteName,
                        items
                );

                future.whenComplete((success, ex) -> {
                    if (ex != null) {
                        log.error("알림 이메일 전송에 실패했습니다. 수신자: {}", member.getEmail(), ex);
                    } else if (!success) {
                        log.error("알림 이메일 전송 실패: {} (반환값: false)", member.getEmail());
                    } else {
                        log.info("알림 이메일 전송 성공: {}, 아이템: {}", member.getEmail(), items.size());
                    }
                });
            } catch (Exception e) {
                log.error("알림 준비에 실패했습니다. 수신자: {}", member.getEmail(), e);
            }
        });
    }

    /**
     * 사이트 이름을 조회합니다. 먼저 구독 정보에서 찾고, 없으면 기본값 사용
     */
    private String getSiteName(Member member, RssFeed rssFeed, String defaultName) {
        return subscriptionRepository.findByMemberAndRssFeed(member, rssFeed)
                .map(subscription -> subscription.getSiteName())
                .orElse(defaultName);
    }

}
