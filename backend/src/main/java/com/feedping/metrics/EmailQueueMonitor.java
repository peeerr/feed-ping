package com.feedping.metrics;

import java.util.concurrent.ThreadPoolExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

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
    @Scheduled(fixedRate = 5000) // 5초마다 실행
    public void monitorQueue() {
        ThreadPoolExecutor executor = emailTaskExecutor.getThreadPoolExecutor();
        int activeCount = executor.getActiveCount();
        int queueSize = executor.getQueue().size();
        int corePoolSize = executor.getCorePoolSize();
        int maxPoolSize = executor.getMaximumPoolSize();
        int poolSize = executor.getPoolSize();
        int largestPoolSize = executor.getLargestPoolSize();
        long completedTaskCount = executor.getCompletedTaskCount();
        int totalPending = activeCount + queueSize;

        // 큐 크기 메트릭 업데이트
        metrics.setEmailQueueSize(totalPending);

        // 스레드풀 활용률 로깅
        log.info("스레드풀 상태: 활성={}/{}, 큐={}/{}, 총 처리중={}, 완료된 작업={}, 최대 사용={}",
                activeCount, poolSize, queueSize, executor.getQueue().remainingCapacity() + queueSize,
                totalPending, completedTaskCount, largestPoolSize);

        // 큐 크기에 따라 경고 로그 출력
        if (queueSize > 50) {
            log.warn("이메일 전송 큐가 과부하 상태입니다. 현재 큐 크기: {}, 활성 작업: {}, 가용 용량: {}",
                    queueSize, activeCount, executor.getQueue().remainingCapacity());
        } else if (queueSize > 20) {
            log.info("이메일 전송 큐 크기가 증가 중입니다. 현재 큐 크기: {}, 활성 작업: {}",
                    queueSize, activeCount);
        }

        // 스레드풀이 포화 상태에 가까워지면 경고
        if (poolSize >= maxPoolSize && queueSize > 50) {
            log.warn("스레드풀이 포화 상태입니다! 최대 스레드 수({})에 도달하고 큐가 {}개 대기 중입니다.",
                    maxPoolSize, queueSize);
        }
    }

}
