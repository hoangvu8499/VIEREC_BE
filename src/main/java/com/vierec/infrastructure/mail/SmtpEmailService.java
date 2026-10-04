package com.vierec.infrastructure.mail;

import com.vierec.config.properties.AppMailProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Sends through the SMTP server of {@code spring.mail.*}. Without {@code spring.mail.host} (developer machines,
 * tests) the email is written to the log instead, so that a reset code can still be used.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmtpEmailService implements EmailService {

    private final ObjectProvider<JavaMailSender> mailSender;
    private final AppMailProperties properties;

    @Value("${spring.mail.host:}")
    private String host;

    @Override
    public boolean send(String to, String subject, String text) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!StringUtils.hasText(host) || sender == null) {
            log.warn("spring.mail.host is not set, email to {} not sent:\n{}\n{}", to, subject, text);
            return true;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.getFrom());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        try {
            sender.send(message);
            return true;
        } catch (MailException ex) {
            log.error("Could not send email \"{}\" to {}", subject, to, ex);
            return false;
        }
    }
}
