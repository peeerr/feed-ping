package com.feedping.service;

import com.feedping.dto.RssItemDto;
import com.feedping.exception.ErrorCode;
import com.feedping.exception.GlobalException;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.parser.txt.CharsetDetector;
import org.apache.tika.parser.txt.CharsetMatch;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * RSS 피드 관련 공통 기능을 제공하는 서비스
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class RssCommonService {

    private final RestTemplate restTemplate;

    /**
     * RSS 피드 URL에서 피드 데이터를 가져와 파싱
     *
     * @param url          RSS 피드 URL
     * @param validateOnly 검증만 수행할지 여부 (true: 검증만, false: 항목 반환)
     * @return RSS 항목 목록 (validateOnly가 true일 경우 빈 리스트)
     * @throws GlobalException RSS 피드 접근 또는 파싱 중 오류 발생 시
     */
    public List<RssItemDto> fetchAndParseRssFeed(String url, boolean validateOnly) {
        try {
            URI uri = UriComponentsBuilder
                    .fromUriString(url)
                    .build(true)  // 이미 인코딩된 상태를 유지
                    .toUri();

            // 피드 데이터 가져오기
            byte[] rawBytes = restTemplate.getForObject(uri, byte[].class);
            if (rawBytes == null) {
                throw new GlobalException(ErrorCode.RSS_FEED_INVALID_FORMAT, "RSS 피드 응답이 비어있습니다.");
            }

            String feedContent = detectAndConvertEncoding(rawBytes);

            try (StringReader reader = new StringReader(feedContent)) {
                SyndFeed feed = new SyndFeedInput().build(reader);

                // 피드 내용 검증 (항목이 없으면 예외)
                if (feed.getEntries().isEmpty()) {
                    throw new GlobalException(ErrorCode.RSS_FEED_INVALID_FORMAT, "RSS 피드에 항목이 없습니다.");
                }

                // 검증 모드인 경우 빈 리스트 반환
                if (validateOnly) {
                    return Collections.emptyList();
                }

                // 항목 반환 모드인 경우 파싱 결과 반환
                return feed.getEntries().stream()
                        .map(RssItemDto::from)
                        .filter(this::isValidRssItem)
                        .limit(20)  // 최대 20개 항목으로 제한
                        .toList();
            }
        } catch (HttpClientErrorException e) {
            log.warn("RSS 피드 접근 권한 오류: {} - {}", url, e.getMessage());

            if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new GlobalException(ErrorCode.RSS_FEED_CONNECTION_ERROR,
                        "RSS 피드에 접근할 권한이 없습니다 (403 Forbidden).");
            } else if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new GlobalException(ErrorCode.RSS_FEED_CONNECTION_ERROR,
                        "RSS 피드를 찾을 수 없습니다 (404 Not Found).");
            } else {
                throw new GlobalException(ErrorCode.RSS_FEED_CONNECTION_ERROR,
                        "RSS 피드 접근 오류: " + e.getStatusCode());
            }
        } catch (HttpServerErrorException e) {
            log.warn("RSS 피드 서버 오류: {} - {}", url, e.getMessage());
            throw new GlobalException(ErrorCode.RSS_FEED_CONNECTION_ERROR,
                    "RSS 피드 서버 오류: " + e.getStatusCode());
        } catch (ResourceAccessException e) {
            log.warn("RSS 피드 접근 실패: {} - {}", url, e.getMessage());
            throw new GlobalException(ErrorCode.RSS_FEED_CONNECTION_ERROR,
                    "RSS 피드에 접근할 수 없습니다. 접속이 거부되었거나 시간 초과되었습니다.");
        } catch (GlobalException e) {
            throw e;
        } catch (Exception e) {
            log.warn("RSS 피드 파싱 실패: {} - {}", url, e.getMessage());
            throw new GlobalException(ErrorCode.RSS_FEED_INVALID_FORMAT,
                    "유효한 RSS 피드 형식이 아닙니다.");
        }
    }

    /**
     * 문자셋 감지 및 변환
     */
    private String detectAndConvertEncoding(byte[] rawBytes) {
        Charset detectedCharset = detectCharset(rawBytes);
        return new String(rawBytes, detectedCharset);
    }

    /**
     * 바이트 배열로부터 문자셋 감지
     */
    private Charset detectCharset(byte[] rawBytes) {
        CharsetDetector detector = new CharsetDetector();
        detector.setText(rawBytes);
        CharsetMatch match = detector.detect();

        if (match != null) {
            log.info("감지된 Charset: {}", match.getName());
            return Charset.forName(match.getName());
        } else {
            log.warn("Charset 감지 실패, 기본값 UTF-8 사용");
            return StandardCharsets.UTF_8;
        }
    }

    /**
     * RSS 항목 유효성 검증
     */
    private boolean isValidRssItem(RssItemDto item) {
        return StringUtils.hasText(item.getTitle()) &&
                StringUtils.hasText(item.getLink());
    }

}
