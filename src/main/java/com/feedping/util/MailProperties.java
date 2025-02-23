package com.feedping.util;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(
        @NotEmpty String username,
        @NotEmpty String senderName,
        Template template
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
