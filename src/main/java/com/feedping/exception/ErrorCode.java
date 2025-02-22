package com.feedping.exception;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ErrorCode {

    NOT_FOUND_MEMBER("사용자를 찾을 수 없습니다.", BAD_REQUEST),
    ALREADY_SUBSCRIBED_RSS("이미 구독한 RSS Feed입니다.", CONFLICT),
    INVALID_TOKEN("유효하지 않은 토큰입니다.", BAD_REQUEST),
    NOT_FOUND_RSS_FEED("존재하지 않는 RSS Feed입니다.", BAD_REQUEST),
    EMAIL_ALREADY_REGISTERED("이미 등록된 이메일입니다.", BAD_REQUEST),

    INVALID_VERIFICATION_TOKEN("유효하지 않은 인증 코드입니다.", BAD_REQUEST),
    ALREADY_SENT_VERIFICATION("이미 인증 이메일을 보냈습니다. 잠시 후 다시 시도하세요.", BAD_REQUEST),

    EMAIL_NOT_VERIFIED("이메일 인증이 필요합니다. 인증 후 30분 이내에 다시 시도해 주세요", BAD_REQUEST);

    private final String message;
    private final HttpStatus status;

}
