package com.yolka.services;

import com.yolka.config.EnvsConfig;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

	private final JavaMailSender mailSender;
	private final EnvsConfig envsConfig;

	public void sendEmail(String to, String subject, String htmlContent) {
		if (envsConfig.getSmtpUser() == null || envsConfig.getSmtpUser().isBlank()) {
			log.info("[MailService Local Mode] To: {} | Subject: {}", to, subject);
			return;
		}
		try {
			MimeMessage message = mailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setFrom(envsConfig.getSmtpUser(), "Yolka Pet Care");
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(htmlContent, true);
			mailSender.send(message);
			log.info("Email sent successfully to {}", to);
		} catch (Exception e) {
			log.error("Error sending email to {}: {}", to, e.getMessage());
		}
	}
}
