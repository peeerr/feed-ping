package com.feedping.metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 이메일 전송 큐 상태를 모니터링하는 컴포넌트
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class EmailQueueMonitor {

    private final NotificationMetrics metrics;

    /**
     * 주기적으로 이메일 큐 상태를 확인하고 메트릭 업데이트 현재는 큐 크기를 모니터링하는 대체 방법 사용
     */
    @Scheduled(fixedRate = 5000) // 5초마다 실행
    public void monitorQueue() {
        // 현재 처리 중인 알림 수를 큐 크기로 대체
        int estimatedQueueSize = metrics.getCurrentlyProcessingCount();

        // 큐 크기 메트릭 업데이트
        metrics.setEmailQueueSize(estimatedQueueSize);

        // 큐 크기에 따라 로그 출력
        if (estimatedQueueSize > 50) {
            log.warn("이메일 처리 큐가 많습니다. 현재 처리 중: {}", estimatedQueueSize);
        } else if (estimatedQueueSize > 20) {
            log.info("이메일 처리가 증가하고 있습니다. 현재 처리 중: {}", estimatedQueueSize);
        }
    }

}
