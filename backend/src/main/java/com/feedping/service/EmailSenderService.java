package com.feedping.service;

import com.feedping.domain.RssItem;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Slf4j
@RequiredArgsConstructor
@Service
public class EmailSenderService {

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
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendVerificationEmail(String to, String code) {
        Context context = new Context();
        context.setVariable("code", code);
        context.setVariable("expirationMinutes", 5);
        String content = templateEngine.process("mail/verification-email", context);

        return sendMailAsync(to, mailProperties.template().verificationSubject(), content);
    }

    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendRssNotification(String to, String siteName, List<RssItem> items) {
        String token = authTokenService.createTokenForEmail(to);

        Context context = new Context();
        context.setVariable("siteName", siteName);
        context.setVariable("items", items);
        context.setVariable("token", token);
        context.setVariable("baseUrl", mailProperties.baseUrl());
        String content = templateEngine.process("mail/rss-notification", context);

        String subject = String.format("[FeedPing] %s에 새 글이 등록되었습니다.", siteName);
        return sendMailAsync(to, subject, content);
    }

}
