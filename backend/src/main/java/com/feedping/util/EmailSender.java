package com.feedping.util;

import com.feedping.domain.RssItem;
import com.feedping.exception.ErrorCode;
import com.feedping.exception.GlobalException;
import com.feedping.service.AuthTokenService;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
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

    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendMailAsync(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailProperties.username(), mailProperties.senderName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
            log.info("이메일 전송에 성공했습니다. 수신자: {}", to);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("이메일 전송에 실패했습니다. 수신자: {}", to, e);
            return CompletableFuture.completedFuture(false);
        }
    }

    private String prepareVerificationEmailContent(String code) {
        Context context = new Context();
        context.setVariable("code", code);
        context.setVariable("expirationMinutes", 5);
        return templateEngine.process("mail/verification-email", context);
    }

    private String prepareRssNotificationContent(String siteName, List<RssItem> items, String token) {
        Context context = new Context();
        context.setVariable("siteName", siteName);
        context.setVariable("items", items);
        context.setVariable("token", token);
        context.setVariable("baseUrl", mailProperties.baseUrl());
        return templateEngine.process("mail/rss-notification", context);
    }

    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendVerificationEmail(String to, String code) {
        String content = prepareVerificationEmailContent(code);
        return sendMailAsync(to, mailProperties.template().verificationSubject(), content);
    }

    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendRssNotification(String to, String siteName, List<RssItem> items) {
        String token = authTokenService.createTokenForEmail(to);
        String content = prepareRssNotificationContent(siteName, items, token);
        String subject = String.format("[FeedPing] %s에 새 글이 등록되었습니다.", siteName);
        return sendMailAsync(to, subject, content);
    }

    // 동기 방식의 메서드도 유지 (기존 코드와의 호환성을 위해)
    public void sendVerificationEmailSync(String to, String code) {
        try {
            String content = prepareVerificationEmailContent(code);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailProperties.username(), mailProperties.senderName());
            helper.setTo(to);
            helper.setSubject(mailProperties.template().verificationSubject());
            helper.setText(content, true);

            mailSender.send(message);
            log.info("인증 이메일 전송에 성공했습니다. 수신자: {}", to);
        } catch (Exception e) {
            log.error("인증 이메일 전송에 실패했습니다. 수신자: {}", to, e);
            throw new GlobalException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

}
