package com.feedping.metrics;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class EmailQueueMonitor {

    private final NotificationMetrics metrics;

    /**
     * 주기적으로 이메일 큐 상태를 확인하고 메트릭 업데이트
     */
    @Scheduled(fixedRate = 5000)
    public void monitorQueue() {
        // 현재 처리 중인 알림 수를 큐 크기의 간접적인 지표로 활용
        int currentlyProcessing = metrics.getCurrentlyProcessingCount();

        // 큐 크기 메트릭 업데이트
        metrics.setEmailQueueSize(currentlyProcessing);

        // 현재 처리 중인 작업 수에 따라 로그 출력
        if (currentlyProcessing > 100) {
            log.warn("대량의 알림이 처리 중입니다. 현재 처리 중: {}", currentlyProcessing);
        } else if (currentlyProcessing > 50) {
            log.info("다수의 알림이 처리 중입니다. 현재 처리 중: {}", currentlyProcessing);
        } else if (currentlyProcessing > 0) {
            log.debug("알림 처리 현황: 현재 처리 중: {}", currentlyProcessing);
        }
    }

}
