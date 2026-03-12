package com.example.Voucher.report;

import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class ReportEmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String reportEmailFrom;

    public ReportEmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${report.email.from}") String reportEmailFrom
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.reportEmailFrom = reportEmailFrom;
    }

    public void sendReportEmail(List<String> recipients, File attachment, String subject, String body) {
        try {
            JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
            if (mailSender == null) {
                throw new IllegalStateException(
                        "JavaMailSender bean is not configured. Set spring.mail.host/port/username/password."
                );
            }
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(reportEmailFrom);
            helper.setTo(recipients.toArray(new String[0]));
            helper.setSubject(subject);
            helper.setText(body, false);
            helper.addAttachment(attachment.getName(), attachment);
            mailSender.send(message);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to send report email: " + ex.getMessage(), ex);
        }
    }
}
