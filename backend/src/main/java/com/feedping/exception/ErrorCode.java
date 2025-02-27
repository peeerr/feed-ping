package com.feedping.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ErrorCode {

    // 리소스를 찾을 수 없는 경우
    NOT_FOUND_MEMBER("사용자를 찾을 수 없습니다.", NOT_FOUND),
    NOT_FOUND_RSS_FEED("존재하지 않는 RSS Feed입니다.", NOT_FOUND),
    SUBSCRIPTION_NOT_FOUND("구독 정보를 찾을 수 없습니다.", NOT_FOUND),

    // 중복/충돌 관련
    ALREADY_SUBSCRIBED_RSS("이미 구독한 RSS Feed입니다.", CONFLICT),

    // 인증/토큰 관련
    INVALID_TOKEN("유효하지 않은 토큰입니다.", UNAUTHORIZED),
    INVALID_VERIFICATION_TOKEN("유효하지 않은 인증 코드입니다.", UNAUTHORIZED),
    EMAIL_NOT_VERIFIED("이메일 인증이 필요합니다. 인증 후 30분 이내에 다시 시도해 주세요", UNAUTHORIZED),

    // 요청 제한/쓰로틀링
    ALREADY_SENT_VERIFICATION("이미 인증 이메일을 보냈습니다. 잠시 후 다시 시도하세요.", TOO_MANY_REQUESTS),

    EMAIL_SEND_FAILED("이메일 전송에 실패했습니다.", INTERNAL_SERVER_ERROR),

    // RSS 관련
    RSS_FEED_PARSING_ERROR("RSS 피드를 가져오거나 파싱하는 중 오류가 발생했습니다.", INTERNAL_SERVER_ERROR),
    RSS_FEED_CONNECTION_ERROR("RSS 피드 서버에 연결할 수 없습니다.", SERVICE_UNAVAILABLE),
    RSS_FEED_INVALID_FORMAT("잘못된 RSS 피드 형식입니다.", BAD_REQUEST),

    // RSS 상세 오류 코드 추가
    RSS_FEED_FORBIDDEN("RSS 피드에 접근할 권한이 없습니다 (403 Forbidden).", BAD_REQUEST),
    RSS_FEED_NOT_FOUND("RSS 피드를 찾을 수 없습니다 (404 Not Found).", BAD_REQUEST),
    RSS_FEED_SERVER_ERROR("RSS 피드 서버 오류가 발생했습니다.", SERVICE_UNAVAILABLE),
    RSS_FEED_NO_ITEMS("RSS 피드에 항목이 없습니다.", BAD_REQUEST),
    RSS_FEED_EMPTY_RESPONSE("RSS 피드 응답이 비어있습니다.", BAD_REQUEST),
    RSS_FEED_ACCESS_ERROR("RSS 피드에 접근할 수 없습니다. 접속이 거부되었거나 시간 초과되었습니다.", SERVICE_UNAVAILABLE);

    private final String message;
    private final HttpStatus status;

}
