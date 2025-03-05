package com.feedping.service;

import com.feedping.domain.RssFeed;
import com.feedping.dto.RssItemDto;
import com.feedping.metrics.NotificationMetrics;
import com.feedping.repository.RssFeedRepository;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class RssFeedSyncService {

    private final RssFeedRepository rssFeedRepository;
    private final RssItemProcessService rssItemProcessService;
    private final RssCommonService rssCommonService;
    private final NotificationMetrics metrics;

    /**
     * 모든 RSS 피드 동기화 (5분 간격)
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void syncAllFeeds() {
        List<RssFeed> rssFeeds = rssFeedRepository.findAll();
        log.info("전체 RSS 피드 동기화 시작: {} 개 피드", rssFeeds.size());

        // 전체 동기화 작업 타이머 시작
        Timer.Sample overallSyncTimer = metrics.startTimer();

        try {
            // 모든 피드에 대한 비동기 작업 시작
            List<CompletableFuture<Boolean>> futures = rssFeeds.stream()
                    .map(this::syncFeedAsync)
                    .collect(Collectors.toList());

            // 모든 작업 완료 대기 (최대 3분)
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .orTimeout(3, TimeUnit.MINUTES)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("일부 피드 동기화가 타임아웃되었습니다", ex);
                        }

                        // 결과 집계
                        long successCount = futures.stream()
                                .filter(f -> !f.isCompletedExceptionally())
                                .filter(f -> {
                                    try {
                                        return f.isDone() && f.get();
                                    } catch (Exception e) {
                                        return false;
                                    }
                                })
                                .count();

                        log.info("전체 RSS 피드 동기화 완료: 성공: {}, 실패: {}",
                                successCount, rssFeeds.size() - successCount);

                        // 전체 동기화 작업 완료 메트릭 기록
                        metrics.stopFeedProcessingTimer(overallSyncTimer);
                    });
        } catch (Exception e) {
            log.error("피드 동기화 스케줄링 중 오류 발생", e);
            metrics.stopFeedProcessingTimer(overallSyncTimer);
        }
    }

    /**
     * 단일 RSS 피드를 비동기적으로 동기화
     */
    @Async("feedSyncExecutor")
    public CompletableFuture<Boolean> syncFeedAsync(RssFeed rssFeed) {
        try {
            syncFeed(rssFeed);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("RSS 피드 비동기 동기화 중 오류 발생: {}", rssFeed.getUrl(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * 단일 RSS 피드 동기화
     */
    public void syncFeed(RssFeed rssFeed) {
        // 단일 피드 동기화 타이머 시작
        Timer.Sample feedTimer = metrics.startTimer();

        try {
            log.info("RSS 피드 동기화 시작: {}", rssFeed.getUrl());

            // 항목 반환 모드로 RSS 피드 파싱 (validateOnly = false)
            List<RssItemDto> fetchedEntries = rssCommonService.fetchAndParseRssFeed(rssFeed.getUrl(), false);

            if (!fetchedEntries.isEmpty()) {
                rssItemProcessService.processNewItems(rssFeed, fetchedEntries);
                log.info("새 항목 발견: {} 개, 피드: {}", fetchedEntries.size(), rssFeed.getUrl());
            } else {
                log.info("새 항목이 없습니다: {}", rssFeed.getUrl());
            }

            // 피드 처리 성공 메트릭 기록
            metrics.recordFeedProcessed();
        } catch (Exception e) {
            // 실패 처리
            metrics.recordFeedFailed();
            log.error("RSS 피드 동기화 중 오류 발생: {}", rssFeed.getUrl(), e);
            throw e;  // 호출자에게 예외 전파 (syncFeedAsync에서 처리)
        } finally {
            // 성공/실패 여부와 관계없이 타이머 종료
            metrics.stopFeedProcessingTimer(feedTimer);
        }
    }

}
