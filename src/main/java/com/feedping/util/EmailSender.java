package com.feedping.util;

import com.feedping.domain.RssItem;
import com.feedping.exception.ErrorCode;
import com.feedping.exception.GlobalException;
import com.feedping.service.AuthTokenService;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Slf4j
@RequiredArgsConstructor
@Component
public class EmailSender {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final MailProperties mailProperties;
    private final AuthTokenService authTokenService;

    private void sendMail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailProperties.username(), mailProperties.senderName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to {}", to, e);
            throw new GlobalException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

    public void sendVerificationEmail(String to, String code) {
        Context context = new Context();
        context.setVariable("code", code);
        context.setVariable("expirationMinutes", 5);

        String content = templateEngine.process("mail/verification-email", context);

        sendMail(to, mailProperties.template().verificationSubject(), content);
    }

    public void sendRssNotification(String to, String siteName, List<RssItem> items) {
        Context context = new Context();
        String token = authTokenService.createTokenForEmail(to);

        context.setVariable("siteName", siteName);
        context.setVariable("items", items);
        context.setVariable("token", token);
        context.setVariable("baseUrl", mailProperties.baseUrl());

        String content = templateEngine.process("mail/rss-notification", context);

        sendMail(to,
                String.format("[FeedPing] %s의 새 글이 등록되었습니다.", siteName),
                content);
    }

}
