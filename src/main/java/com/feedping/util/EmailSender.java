package com.feedping.util;

import com.feedping.exception.ErrorCode;
import com.feedping.exception.GlobalException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@RequiredArgsConstructor
@Component
public class EmailSender {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final MailProperties mailProperties;

    public void sendVerificationEmail(String to, String code) {
        Context context = new Context();
        context.setVariable("code", code);
        context.setVariable("expirationMinutes", 5);

        String content = templateEngine.process("mail/verification-email", context);

        sendMail(to, mailProperties.template().verificationSubject(), content);
    }

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
            throw new GlobalException(ErrorCode.EMAIL_SEND_FAILED);
        }
    }

}
