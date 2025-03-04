package com.feedping.service;

import com.feedping.domain.RssItem;
import com.feedping.metrics.NotificationMetrics;
import io.micrometer.core.instrument.Timer;
import jakarta.mail.internet.MimeMessage;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.mail.MailException;
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

            log.info("이메일 전송 성공: {}", to);
            return CompletableFuture.completedFuture(true);
        } catch (TaskRejectedException e) {
            // 작업 거부 오류 명시적 처리
            metrics.recordTaskRejected();
            log.error("이메일 전송 작업이 큐 포화로 거부됨: {}", to, e);
            return CompletableFuture.completedFuture(false);
        } catch (MailException e) {
            // 메일 전송 관련 오류
            String errorType = e.getClass().getSimpleName();
            metrics.recordEmailFailedByReason("메일오류-" + errorType);
            log.error("메일 서버 오류: {}", to, e);
            return CompletableFuture.completedFuture(false);
        } catch (Exception e) {
            // 기타 오류
            String errorType = e.getClass().getSimpleName();
            metrics.recordEmailFailedByReason(errorType);
            log.error("이메일 전송 실패: {}", to, e);
            return CompletableFuture.completedFuture(false);
        }
    }

    @Async("emailTaskExecutor")
    public CompletableFuture<Boolean> sendVerificationEmail(String to, String code) {
        try {
            Timer.Sample overallTimer = metrics.startTimer();

            Context context = new Context();
            context.setVariable("code", code);
            context.setVariable("expirationMinutes", 5);
            String content = templateEngine.process("mail/verification-email", context);

            // 타이머 중지
            metrics.stopEmailSendingTimer(overallTimer);

            return sendMailAsync(to, mailProperties.template().verificationSubject(), content);
        } catch (TaskRejectedException e) {
            metrics.recordTaskRejected();
            log.error("인증 이메일 전송 작업이 큐 포화로 거부되었습니다: {}", to, e);
            return CompletableFuture.completedFuture(false);
        } catch (Exception e) {
            log.error("인증 이메일 준비 중 오류: {}", to, e);
            metrics.recordEmailFailedByReason("VerificationPreparationError");
            return CompletableFuture.completedFuture(false);
        }
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
        } catch (TaskRejectedException e) {
            metrics.recordTaskRejected();
            log.error("알림 이메일 전송 작업이 큐 포화로 거부되었습니다: {}", to, e);
            return CompletableFuture.completedFuture(false);
        } catch (Exception e) {
            metrics.recordEmailFailedByReason("NotificationPreparationError");
            log.error("알림 이메일 템플릿 처리 중 오류: {}", to, e);
            return CompletableFuture.completedFuture(false);
        }
    }

}
