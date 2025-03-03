package com.feedping.service;

import com.feedping.domain.RssItem;
import com.feedping.metrics.NotificationMetrics;
import io.micrometer.core.instrument.Timer;
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
    private final NotificationMetrics metrics;

    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendMailAsync(String to, String subject, String content) {
        Timer.Sample emailTimer = metrics.startTimer();

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailProperties.username(), mailProperties.senderName());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);

            // 전송 성공 기록
            metrics.stopEmailSendingTimer(emailTimer);
            metrics.recordEmailSent();

            log.info("이메일 전송에 성공했습니다. 수신자: {}", to);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            // 실패 기록
            String errorType = e.getClass().getSimpleName();
            metrics.recordEmailFailedByReason(errorType);

            log.error("이메일 전송에 실패했습니다. 수신자: {}", to, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendVerificationEmail(String to, String code) {
        Timer.Sample overallTimer = metrics.startTimer();

        Context context = new Context();
        context.setVariable("code", code);
        context.setVariable("expirationMinutes", 5);
        String content = templateEngine.process("mail/verification-email", context);

        // 타이머 중지
        metrics.stopEmailSendingTimer(overallTimer);

        return sendMailAsync(to, mailProperties.template().verificationSubject(), content);
    }

    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendRssNotification(String to, String siteName, List<RssItem> items) {
        Timer.Sample processTimer = metrics.startTimer();

        try {
            String token = authTokenService.createTokenForEmail(to);

            Context context = new Context();
            context.setVariable("siteName", siteName);
            context.setVariable("items", items);
            context.setVariable("token", token);
            context.setVariable("baseUrl", mailProperties.baseUrl());
            String content = templateEngine.process("mail/rss-notification", context);

            String subject = String.format("[FeedPing] %s에 새 글이 등록되었습니다.", siteName);

            // 타이머 종료 (템플릿 처리 시간 기록)
            metrics.stopProcessingTimer(processTimer);

            return sendMailAsync(to, subject, content);
        } catch (Exception e) {
            metrics.recordEmailFailedByReason("TemplateProcessingError");
            log.error("알림 이메일 템플릿 처리 중 오류 발생: {}", to, e);
            return CompletableFuture.failedFuture(e);
        }
    }

}
