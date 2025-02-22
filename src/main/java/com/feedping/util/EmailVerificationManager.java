package com.feedping.util;

import com.feedping.exception.ErrorCode;
import com.feedping.exception.GlobalException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class EmailVerificationManager {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String EMAIL_VERIFICATION_PREFIX = "email_verification:";
    private static final String VERIFIED_EMAIL_PREFIX = "verified_email:";
    private static final long VERIFICATION_EXPIRATION_TIME = 5 * 60; // 5분
    private static final long VERIFIED_EMAIL_EXPIRATION_TIME = 30 * 60; // 30분
    private static final Random random = new Random();

    public String createCode(String email) {
        String code = generateCode();
        redisTemplate.opsForValue().set(EMAIL_VERIFICATION_PREFIX + email, code, VERIFICATION_EXPIRATION_TIME, TimeUnit.SECONDS);
        return code;
    }

    public void validateCode(String email, String code) {
        String key = EMAIL_VERIFICATION_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode != null && storedCode.equals(code)) {
            redisTemplate.delete(key);
            // 인증 완료된 이메일 저장
            redisTemplate.opsForValue().set(VERIFIED_EMAIL_PREFIX + email, "verified", VERIFIED_EMAIL_EXPIRATION_TIME, TimeUnit.SECONDS);
            return;
        }

        throw new GlobalException(ErrorCode.INVALID_VERIFICATION_TOKEN);
    }

    public boolean isEmailVerified(String email) {
        String key = VERIFIED_EMAIL_PREFIX + email;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    private String generateCode() {
        return String.format("%06d", random.nextInt(1000000));
    }

}
