package com.feedping.service;

import com.feedping.domain.RssFeed;
import com.feedping.dto.RssItemDto;
import com.feedping.exception.ErrorCode;
import com.feedping.exception.GlobalException;
import com.feedping.repository.RssFeedRepository;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import java.io.StringReader;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@RequiredArgsConstructor
@Service
public class RssFeedSyncService {

    private final RssFeedRepository rssFeedRepository;
    private final RestTemplate restTemplate;
    private final RssItemProcessService rssItemProcessService;

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

    public void syncFeed(RssFeed rssFeed) {
        List<RssItemDto> fetchedEntries = fetchAndParseRssFeed(rssFeed.getUrl());

        if (!fetchedEntries.isEmpty()) {
            rssItemProcessService.processNewItems(rssFeed, fetchedEntries);
        }
    }

    private List<RssItemDto> fetchAndParseRssFeed(String url) {
        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(url)
                    .build(true)  // 이미 인코딩된 상태를 유지
                    .toUri();

            // 여기서 대용량 데이터 문제가 발생할 수 있음
            // 향후 개선 필요: 스트림 방식 파싱 또는 응답 크기 제한 구현
            String feedContent = restTemplate.getForObject(uri, String.class);

            try (StringReader reader = new StringReader(feedContent)) {
                SyndFeed feed = new SyndFeedInput().build(reader);

                return feed.getEntries().stream()
                        .map(RssItemDto::from)
                        .filter(this::isValidRssItem)
                        .limit(20)  // 최대 20개 항목으로 제한
                        .toList();
            }
        } catch (Exception e) {
            log.error("RSS 피드 가져오기 또는 파싱에 실패했습니다. URL: {}", url, e);
            throw new GlobalException(ErrorCode.RSS_FEED_PARSING_ERROR);
        }
    }

    private boolean isValidRssItem(RssItemDto item) {
        return StringUtils.hasText(item.getTitle()) &&
                StringUtils.hasText(item.getLink());
    }

}
