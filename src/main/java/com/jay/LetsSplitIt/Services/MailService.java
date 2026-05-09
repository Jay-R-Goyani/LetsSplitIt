package com.jay.LetsSplitIt.Services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final boolean enabled;

    MailService(JavaMailSender mailSender,
                @Value("${app.mail.from:}") String fromAddress,
                @Value("${app.mail.enabled:true}") boolean enabled) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.enabled = enabled;
    }

    public void sendSimpleMail(String to, String subject, String body) {
        if (!enabled) {
            log.info("Mail disabled; skipping mail to {}", to);
            return;
        }
        if (to == null || to.isBlank()) {
            log.warn("Skipping mail with blank recipient (subject={})", subject);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            if (fromAddress != null && !fromAddress.isBlank()) {
                msg.setFrom(fromAddress);
            }
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("Sent mail to {} (subject={})", to, subject);
        } catch (Exception ex) {
            log.error("Failed to send mail to {} (subject={}): {}", to, subject, ex.getMessage());
        }
    }
}