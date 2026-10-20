package com.yolka.helpers;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.yolka.config.EnvsConfig;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class FirebaseAdminHelper {

	private final EnvsConfig envsConfig;

	@Getter
	private boolean initialized = false;

	@PostConstruct
	public void initialize() {
		try {
			if (!FirebaseApp.getApps().isEmpty()) {
				initialized = true;
				return;
			}
			File credentialsFile = envsConfig.resolveFile(envsConfig.getFirebaseAdminCredentialsJsonPath());
			if (credentialsFile == null || !credentialsFile.exists()) {
				log.warn("Firebase Admin credentials file not found at configured path. Push notifications will be logged only until configured.");
				return;
			}
			try (FileInputStream serviceAccount = new FileInputStream(credentialsFile)) {
				FirebaseOptions options = FirebaseOptions.builder()
						.setCredentials(GoogleCredentials.fromStream(serviceAccount))
						.build();
				FirebaseApp.initializeApp(options);
				initialized = true;
				log.info("Firebase Admin SDK connected successfully");
			}
		} catch (Exception e) {
			log.error("Firebase connection error: {}", e.getMessage());
		}
	}

	public String sendPushNotification(String fcmToken, String title, String body, Map<String, String> data) {
		if (fcmToken == null || fcmToken.isBlank()) {
			return null;
		}
		if (!initialized) {
			log.info("[FCM Simulation] To token={} | title='{}' | body='{}'", fcmToken, title, body);
			return "simulated-message-id";
		}
		try {
			Message.Builder builder = Message.builder()
					.setToken(fcmToken)
					.setNotification(Notification.builder()
							.setTitle(title)
							.setBody(body)
							.build());
			if (data != null && !data.isEmpty()) {
				builder.putAllData(data);
			}
			return FirebaseMessaging.getInstance().send(builder.build());
		} catch (Exception e) {
			log.error("Error sending FCM message: {}", e.getMessage());
			throw CustomError.internalServer("Error sending FCM push notification: " + e.getMessage(), "FCM_SEND_ERROR");
		}
	}
}
