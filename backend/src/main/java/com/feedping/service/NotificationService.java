package com.feedping.service;

import com.feedping.domain.Member;
import com.feedping.domain.RssFeed;
import com.feedping.domain.RssItem;
import com.feedping.event.RssNotificationEvent;
import com.feedping.metrics.NotificationMetrics;
import com.feedping.notification.NotificationTask;
import com.feedping.notification.PriorityNotificationQueue;
import com.feedping.repository.SubscriptionRepository;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Service
public class NotificationService {

    private final PriorityNotificationQueue notificationQueue;
    private final EmailSenderService emailSenderService;
    private final SubscriptionRepository subscriptionRepository;
    private final NotificationMetrics metrics;

    private ThreadPoolExecutor workerPool;
    private ScheduledExecutorService scheduledExecutor;

    @Value("${notification.batch-size:50}")
    private int batchSize;

    @Value("${notification.timeout-seconds:30}")
    private int timeoutSeconds;

    @Value("${notification.worker-threads:5}")
    private int workerThreads;

    @Value("${notification.max-retry-count:3}")
    private int maxRetryCount;

    // 재시도 대기 시간 (밀리초)
    private final int[] retryDelays = {5000, 30000, 120000};  // 5초, 30초, 2분

    @PostConstruct
    public void initialize() {
        // 워커 스레드 풀 초기화
        workerPool = new ThreadPoolExecutor(
                workerThreads, workerThreads,
                60L, TimeUnit.SECONDS,
                new java.util.concurrent.LinkedBlockingQueue<>(500),  // 큐 크기 제한
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r, "notification-worker-" + counter.getAndIncrement());
                        thread.setDaemon(true);  // 애플리케이션 종료 시 같이 종료되도록
                        return thread;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()  // 큐가 가득 차면 호출 스레드에서 실행
        );

        // 스케줄러 초기화
        scheduledExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "notification-scheduler");
            thread.setDaemon(true);
            return thread;
        });

        // 주기적으로 큐 처리
        scheduledExecutor.scheduleWithFixedDelay(
                this::processNotificationQueue,
                timeoutSeconds, timeoutSeconds, TimeUnit.SECONDS
        );

        log.info("알림 서비스 초기화 완료: 배치크기={}, 워커스레드={}, 타임아웃={}초",
                batchSize, workerThreads, timeoutSeconds);
    }

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRssNotificationEvent(RssNotificationEvent event) {
        Timer.Sample eventTimer = metrics.startTimer();

        try {
            Map<Member, List<RssItem>> notificationMap = event.getNotificationMap();
            int totalNotifications = notificationMap.values().stream()
                    .mapToInt(List::size)
                    .sum();

            // 처리 시작 시 한 번만 증가
            metrics.incrementCurrentlyProcessing(totalNotifications);

            try {
                log.info("RSS 알림 이벤트 수신: 구독자 {} 명, 항목 {} 개",
                        notificationMap.size(), totalNotifications);

                // 알림 작업 생성 및 큐에 추가
                notificationMap.forEach((member, items) -> {
                    try {
                        RssFeed rssFeed = items.get(0).getRssFeed();
                        String siteName = getSiteName(member, rssFeed, event.getSiteName());

                        NotificationTask task = NotificationTask.builder()
                                .email(member.getEmail())
                                .rssFeedId(rssFeed.getId())
                                .siteName(siteName)
                                .items(new ArrayList<>(items))
                                .retryCount(0)
                                .build();

                        notificationQueue.addNotification(task);
                    } catch (Exception e) {
                        log.error("알림 작업 생성 실패: {}", member.getEmail(), e);
                    }
                });

                // 알림 메트릭 업데이트
                metrics.recordQueued(totalNotifications);

                // 큐 크기가 일정 이상이면 즉시 처리 트리거
                checkQueueSizeAndProcess();

            } finally {
                // 항상 처리 중 카운터 감소 보장
                metrics.decrementCurrentlyProcessing(totalNotifications);
            }

            // 이벤트 처리 시간 기록
            metrics.stopProcessingTimer(eventTimer);
        } catch (Exception e) {
            log.error("알림 이벤트 처리 실패", e);
            // 타이머 종료 보장
            metrics.stopProcessingTimer(eventTimer);
        }
    }

    private void checkQueueSizeAndProcess() {
        if (notificationQueue.getTotalSize() >= batchSize) {
            processNotificationQueue();
        }
    }

    /**
     * 큐 상태 및 워커 풀 모니터링
     */
    @Scheduled(fixedRate = 10000) // 10초마다 실행
    public void monitorQueueAndWorkerStatus() {
        // 큐 상태 업데이트
        notificationQueue.updateMetrics();

        // 우선순위별 큐 크기 로깅
        Map<PriorityNotificationQueue.Priority, Integer> queueSizes = notificationQueue.getQueueSizes();
        log.info("알림 큐 상태: 전체={}, 상={}, 중={}, 하={}",
                notificationQueue.getTotalSize(),
                queueSizes.get(PriorityNotificationQueue.Priority.HIGH),
                queueSizes.get(PriorityNotificationQueue.Priority.MEDIUM),
                queueSizes.get(PriorityNotificationQueue.Priority.LOW));

        // 워커 풀 상태 로깅
        int activeCount = workerPool.getActiveCount();
        int poolSize = workerPool.getPoolSize();
        int queueSize = workerPool.getQueue().size();
        long completedTasks = workerPool.getCompletedTaskCount();

        log.info("워커 풀 상태: 활성={}/{}, 큐={}, 완료된 작업={}",
                activeCount, poolSize, queueSize, completedTasks);
    }

    private synchronized void processNotificationQueue() {
        int totalSize = notificationQueue.getTotalSize();
        if (totalSize == 0) {
            return;
        }

        log.debug("알림 큐 처리 시작: {} 개의 작업", totalSize);

        try {
            // 우선순위에 따라 작업 추출
            List<NotificationTask> batch = notificationQueue.drainTasks(batchSize);

            if (!batch.isEmpty()) {
                // 배치 처리 메트릭 기록
                metrics.recordBatchProcessed(batch.size());

                // 피드 ID별로 그룹화
                Map<Long, List<NotificationTask>> tasksByFeed = batch.stream()
                        .collect(Collectors.groupingBy(NotificationTask::getRssFeedId));

                tasksByFeed.forEach((feedId, tasks) -> {
                    try {
                        // 각 피드별 작업을 워커 스레드풀에 제출
                        workerPool.submit(() -> processTaskGroup(tasks));
                    } catch (TaskRejectedException e) {
                        log.warn("Worker pool rejected tasks for feed {}, requeuing", feedId);
                        metrics.recordTaskRejected();

                        // 작업을 다시 큐에 넣기
                        tasks.forEach(notificationQueue::addNotification);
                    }
                });
            }
        } catch (Exception e) {
            log.error("Error processing notification queue", e);
        }
    }

    private void processTaskGroup(List<NotificationTask> tasks) {
        if (tasks.isEmpty()) {
            return;
        }

        Long feedId = tasks.get(0).getRssFeedId();
        String siteName = tasks.get(0).getSiteName();
        int taskCount = tasks.size();

        // 메트릭 증가
        metrics.incrementCurrentlyProcessing(taskCount);
        Timer.Sample batchTimer = metrics.startTimer();

        try {
            log.info("피드 {} ({})에 대한 {} 개의 알림 처리 시작", feedId, siteName, taskCount);

            // 이메일별로 그룹화하여 처리
            Map<String, NotificationTask> tasksByEmail = new ConcurrentHashMap<>();
            tasks.forEach(task -> tasksByEmail.put(task.getEmail(), task));

            // 각 이메일별로 개별 처리
            for (NotificationTask task : tasksByEmail.values()) {
                try {
                    // 이메일 전송 로직...
                } catch (Exception e) {
                    handleFailedTask(task, e);
                }
            }

            log.info("피드 {}에 대한 {} 개의 알림 처리 완료", feedId, taskCount);
        } catch (Exception e) {
            log.error("피드 {}의 알림 배치 처리 중 오류 발생", feedId, e);
        } finally {
            // 항상 메트릭 감소 및 타이머 종료
            metrics.decrementCurrentlyProcessing(taskCount);
            metrics.stopBatchProcessingTimer(batchTimer);
        }
    }

    private void handleFailedTask(NotificationTask task, Throwable error) {
        metrics.recordFailed(1);

        // 오류 유형에 따른 메트릭 기록
        if (error instanceof TaskRejectedException) {
            metrics.recordTaskRejected();
        } else {
            metrics.recordEmailFailedByReason(error.getClass().getSimpleName());
        }

        // 최대 재시도 횟수 미만이면 재시도 큐에 추가
        if (task.getRetryCount() < maxRetryCount) {
            scheduleRetry(task);
        } else {
            log.error("{} 명의 수신자에게 알림 전송 실패: {} 회 재시도 후 최종 실패: {}",
                    task.getEmail(), task.getRetryCount(), error.getMessage());
        }
    }

    private void scheduleRetry(NotificationTask task) {
        int retryCount = task.getRetryCount();
        int delay = retryDelays[Math.min(retryCount, retryDelays.length - 1)];

        NotificationTask retryTask = task.withIncrementedRetryCount();

        // 재시도 메트릭 기록
        metrics.recordRetryAttempt();

        log.info("알림 재시도 #{} 예약: 수신자={}, 지연={}ms",
                retryCount + 1, task.getEmail(), delay);

        // 지연 후 재시도
        scheduledExecutor.schedule(
                () -> notificationQueue.addNotification(retryTask),
                delay, TimeUnit.MILLISECONDS
        );
    }

    private String getSiteName(Member member, RssFeed rssFeed, String defaultName) {
        return subscriptionRepository.findByMemberAndRssFeed(member, rssFeed)
                .map(subscription -> subscription.getSiteName())
                .orElse(defaultName);
    }

    @PreDestroy
    public void shutdown() {
        log.info("알림 서비스 종료 중");
        scheduledExecutor.shutdown();
        workerPool.shutdown();

        try {
            if (!workerPool.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("워커 풀이 제한 시간 내에 종료되지 않았습니다");
                workerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.warn("워커 풀 종료 중 인터럽트 발생", e);
            workerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}
