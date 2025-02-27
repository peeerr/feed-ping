package com.feedping.service;

import com.feedping.domain.RssFeed;
import com.feedping.dto.RssItemDto;
import com.feedping.repository.RssFeedRepository;
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

    /**
     * 모든 RSS 피드 동기화 (5분 간격)
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void syncAllFeeds() {
        List<RssFeed> rssFeeds = rssFeedRepository.findAll();

        for (RssFeed rssFeed : rssFeeds) {
            try {
                syncFeed(rssFeed);
            } catch (Exception e) {
                log.error("RSS 피드 동기화에 실패했습니다. URL: {}", rssFeed.getUrl(), e);
            }
        }
    }

    /**
     * 단일 RSS 피드 동기화
     */
    public void syncFeed(RssFeed rssFeed) {
        try {
            // 항목 반환 모드로 RSS 피드 파싱 (validateOnly = false)
            List<RssItemDto> fetchedEntries = rssCommonService.fetchAndParseRssFeed(rssFeed.getUrl(), false);

            if (!fetchedEntries.isEmpty()) {
                rssItemProcessService.processNewItems(rssFeed, fetchedEntries);
            }
        } catch (Exception e) {
            log.error("RSS 피드 동기화 중 오류 발생: {}", rssFeed.getUrl(), e);
            throw e;
        }
    }

}
