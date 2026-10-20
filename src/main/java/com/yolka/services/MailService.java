package com.yolka.services;

import com.yolka.config.EnvsConfig;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

	private final EnvsConfig envsConfig;

	public void sendEmail(String to, String subject, String htmlContent) {
		String rawUser = envsConfig.getSmtpUser() != null ? envsConfig.getSmtpUser().replace("\"", "").trim() : "";
		String rawPass = envsConfig.getSmtpPass() != null ? envsConfig.getSmtpPass().replace("\"", "").replace(" ", "").trim() : "";

		if (rawUser.isBlank() || rawPass.isBlank() || rawUser.contains("tu_correo") || rawPass.contains("tu_app_password")) {
			log.info("[MailService Modo Desarrollo] Correo simulado para: {} | Asunto: {}", to, subject);
			return;
		}

		try {
			JavaMailSenderImpl sender = new JavaMailSenderImpl();
			sender.setHost(envsConfig.getSmtpHost().replace("\"", "").trim());
			sender.setPort(envsConfig.getSmtpPort());
			sender.setUsername(rawUser);
			sender.setPassword(rawPass);
			sender.setDefaultEncoding("UTF-8");

			Properties props = sender.getJavaMailProperties();
			props.put("mail.transport.protocol", "smtp");
			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.starttls.required", "true");
			props.put("mail.smtp.ssl.trust", sender.getHost());

			MimeMessage message = sender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
			helper.setFrom(rawUser, "Yolka Pet Care");
			helper.setTo(to);
			helper.setSubject(subject);
			helper.setText(htmlContent, true);
			sender.send(message);
			log.info("Email sent successfully to {}", to);
		} catch (Exception e) {
			log.error("Error sending email to {}: {}. Verifica que SMTP_USER y SMTP_PASS (Contraseña de Aplicación de 16 letras de Google) sean válidos en .env", to, e.getMessage());
		}
	}
}
