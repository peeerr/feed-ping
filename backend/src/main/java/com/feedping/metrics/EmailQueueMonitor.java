package com.feedping.metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

/**
 * 이메일 전송 큐 상태를 모니터링하는 컴포넌트
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class EmailQueueMonitor {

    @Qualifier("emailTaskExecutor")
    private final ThreadPoolTaskExecutor emailTaskExecutor;
    private final NotificationMetrics metrics;

    /**
     * 주기적으로 이메일 큐 상태를 확인하고 메트릭 업데이트
     */
    @Scheduled(fixedRate = 5000)
    public void monitorQueue() {
        int activeCount = emailTaskExecutor.getActiveCount();
        int queueSize = emailTaskExecutor.getThreadPoolExecutor().getQueue().size();
        int totalPending = activeCount + queueSize;

        metrics.setEmailQueueSize(totalPending);

        // 큐 크기에 따라 로그 출력
        if (queueSize > 50) {
            log.warn("이메일 전송 큐가 과부하 상태입니다. 현재 큐 크기: {}, 활성 작업: {}", queueSize, activeCount);
        } else if (queueSize > 20) {
            log.info("이메일 전송 큐가 증가하고 있습니다. 현재 큐 크기: {}, 활성 작업: {}", queueSize, activeCount);
        }
    }

}
