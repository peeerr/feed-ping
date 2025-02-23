package com.feedping.util;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(
        @NotEmpty String username,
        @NotEmpty String senderName,
        Template template,
//        @Pattern(regexp = "^https?://.*")
        String baseUrl
) {
    public MailProperties {
        template = template != null ? template : new Template(
                "[FeedPing] 이메일 인증을 완료해주세요"
        );
    }

    public record Template(
            String verificationSubject
    ) {
        public Template {
            verificationSubject = verificationSubject != null
                    ? verificationSubject
                    : "[FeedPing] 이메일 인증을 완료해주세요";
        }
    }
}
