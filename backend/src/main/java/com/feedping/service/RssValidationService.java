package com.feedping.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * RSS 피드 URL 유효성 검증 서비스
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RssValidationService {

    private final RssCommonService rssCommonService;

    /**
     * RSS 피드 URL의 유효성 검사
     *
     * @param url RSS URL
     */
    public void validateRssUrl(String url) {
        // 검증 모드로 RSS 피드 파싱 (validateOnly = true)
        rssCommonService.fetchAndParseRssFeed(url, true);
        // 예외가 발생하지 않으면 유효한 RSS 피드로 간주
    }

}
