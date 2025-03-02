package com.feedping.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import com.feedping.exception.ErrorCode;
import com.feedping.exception.GlobalException;
import java.util.Collections;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RssValidationServiceTest {

    @InjectMocks
    private RssValidationService rssValidationService;

    @Mock
    private RssCommonService rssCommonService;

    @Test
    @DisplayName("유효한 RSS URL은 검증에 성공한다")
    void should_ValidateSuccessfully_When_ValidRssUrl() {
        // given
        String validRssUrl = "https://valid-rss-feed.com/rss.xml";
        given(rssCommonService.fetchAndParseRssFeed(validRssUrl, true)).willReturn(Collections.emptyList());

        // when & then
        assertThatCode(() -> rssValidationService.validateRssUrl(validRssUrl))
                .doesNotThrowAnyException();

        then(rssCommonService).should().fetchAndParseRssFeed(validRssUrl, true);
    }

    @Test
    @DisplayName("유효하지 않은 RSS URL은 검증 시 예외가 발생한다")
    void should_ThrowException_When_InvalidRssUrl() {
        // given
        String invalidRssUrl = "https://invalid-rss-feed.com/rss.xml";
        GlobalException exception = new GlobalException(ErrorCode.RSS_FEED_INVALID_FORMAT, "유효하지 않은 RSS 피드 형식입니다.");

        willThrow(exception).given(rssCommonService).fetchAndParseRssFeed(invalidRssUrl, true);

        // when & then
        assertThatThrownBy(() -> rssValidationService.validateRssUrl(invalidRssUrl))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RSS_FEED_INVALID_FORMAT);

        then(rssCommonService).should().fetchAndParseRssFeed(invalidRssUrl, true);
    }

    @Test
    @DisplayName("연결할 수 없는 RSS URL은 검증 시 예외가 발생한다")
    void should_ThrowException_When_RssFeedConnectionError() {
        // given
        String unreachableRssUrl = "https://unreachable-rss-feed.com/rss.xml";
        GlobalException exception = new GlobalException(ErrorCode.RSS_FEED_CONNECTION_ERROR, "RSS 피드 서버에 연결할 수 없습니다.");

        willThrow(exception).given(rssCommonService).fetchAndParseRssFeed(unreachableRssUrl, true);

        // when & then
        assertThatThrownBy(() -> rssValidationService.validateRssUrl(unreachableRssUrl))
                .isInstanceOf(GlobalException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RSS_FEED_CONNECTION_ERROR);

        then(rssCommonService).should().fetchAndParseRssFeed(unreachableRssUrl, true);
    }

}
