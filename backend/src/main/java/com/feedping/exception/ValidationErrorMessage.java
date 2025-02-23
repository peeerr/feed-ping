package com.feedping.exception;

public class ValidationErrorMessage {

    public static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    public static final String VALIDATION_ERROR = "입력 데이터의 유효성을 검사하던 중 문제가 발생했습니다.";

    public static final String INVALID_EMAIL = "올바른 이메일 형식이 아닙니다.";
    public static final String INVALID_URL = "올바른 URL 형식이 아닙니다.";
    public static final String INVALID_VERIFICATION_CODE = "유효하지 않은 인증 코드입니다.";

    public static final String EMPTY_EMAIL = "이메일은 필수 입력 값입니다.";
    public static final String EMPTY_URL = "RSS URL은 필수 입력 값입니다.";
    public static final String EMPTY_SITE_NAME = "사이트 이름은 필수 입력 값입니다.";
    public static final String EMPTY_CODE = "인증 코드를 입력 해주세요.";

    public static final String MAX_LENGTH = "최대 255자까지 입력 가능합니다.";
    public static final String URL_MAX_LENGTH = "URL은 최대 4096자까지 입력 가능합니다.";

    public static final String EMPTY_TOKEN = "유효한 토큰이 필요합니다.";

}
