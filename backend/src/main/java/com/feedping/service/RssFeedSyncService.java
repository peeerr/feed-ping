package com.feedping.service;

import com.feedping.domain.RssFeed;
import com.feedping.dto.RssItemDto;
import com.feedping.metrics.NotificationMetrics;
import com.feedping.repository.RssFeedRepository;
import io.micrometer.core.instrument.Timer;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        int successCount = 0;
        int failCount = 0;

        for (RssFeed rssFeed : rssFeeds) {
            try {
                // 별도 스레드에서 동기화 실행 (병렬 처리를 위해)
                syncFeedAsync(rssFeed);
                successCount++;
                metrics.recordFeedProcessed();
            } catch (Exception e) {
                failCount++;
                metrics.recordFeedFailed();
                log.error("RSS 피드 동기화에 실패했습니다. URL: {}", rssFeed.getUrl(), e);
            }
        }

        // 전체 동기화 작업 완료 메트릭 기록
        metrics.stopFeedProcessingTimer(overallSyncTimer);
        log.info("전체 RSS 피드 동기화 완료: 성공: {}, 실패: {}", successCount, failCount);
    }

    /**
     * 단일 RSS 피드를 비동기적으로 동기화
     */
    private void syncFeedAsync(RssFeed rssFeed) {
        try {
            syncFeed(rssFeed);
        } catch (Exception e) {
            log.error("RSS 피드 비동기 동기화 중 오류 발생: {}", rssFeed.getUrl(), e);
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
            metrics.stopFeedProcessingTimer(feedTimer);
        } catch (Exception e) {
            // 실패 처리
            metrics.stopFeedProcessingTimer(feedTimer);
            log.error("RSS 피드 동기화 중 오류 발생: {}", rssFeed.getUrl(), e);
            throw e;
        }
    }

}
