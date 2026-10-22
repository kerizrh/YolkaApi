package com.yolka.config;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Configuration
public class EnvsConfig {

	@Value("${PORT:}")
	private int port;

	@Value("${BASE_URL:}")
	private String baseUrl;

	@Value("${JWT_SECRET:}")
	private String jwtSecret;

	@Value("${ENCRYPTION_KEY:}")
	private String encryptionKey;

	@Value("${FIREBASE_ADMIN_CREDENTIALS_JSON_PATH:}")
	private String firebaseAdminCredentialsJsonPath;

	@Value("${GOOGLE_OAUTH_JSON_PATH:}")
	private String googleOauthJsonPath;

	@Value("${GOOGLE_WEB_CLIENT_ID:}")
	private String googleWebClientId;

	@Value("${GOOGLE_WEB_CLIENT_SECRET:}")
	private String googleWebClientSecret;

	@Value("${GOOGLE_REDIRECT_URI:}")
	private String googleRedirectUri;

	@Value("${BACKUP_STORAGE_PATH:}")
	private String backupStoragePath;

	@Value("${GOOGLE_DRIVE_FOLDER_ID:}")
	private String googleDriveFolderId;

	@Value("${GOOGLE_DRIVE_CREDENTIALS_JSON_PATH:}")
	private String googleDriveCredentialsJsonPath;

	@Value("${SMTP_HOST:smtp.gmail.com}")
	private String smtpHost;

	@Value("${SMTP_PORT:587}")
	private int smtpPort;

	@Value("${SMTP_USER:}")
	private String smtpUser;

	@Value("${SMTP_PASS:}")
	private String smtpPass;

	private final List<String> allowedGoogleClientIds = new ArrayList<>();

	@PostConstruct
	public void init() {
		if (googleWebClientId != null && !googleWebClientId.isBlank()) {
			allowedGoogleClientIds.add(googleWebClientId.trim());
		}

		File oauthFile = resolveFile(googleOauthJsonPath);
		if (oauthFile != null && oauthFile.exists()) {
			try {
				ObjectMapper mapper = new ObjectMapper();
				JsonNode root = mapper.readTree(Files.readString(oauthFile.toPath()));
				JsonNode node = root.has("web") ? root.get("web") : root.get("installed");
				if (node != null) {
					if (node.hasNonNull("client_id")) {
						String cid = node.get("client_id").asText();
						if (!cid.isBlank() && !allowedGoogleClientIds.contains(cid)) {
							allowedGoogleClientIds.add(cid);
						}
						if (googleWebClientId == null || googleWebClientId.isBlank()) {
							googleWebClientId = cid;
						}
					}
					if (node.hasNonNull("client_secret") && (googleWebClientSecret == null || googleWebClientSecret.isBlank())) {
						googleWebClientSecret = node.get("client_secret").asText();
					}
				}
				log.info("Google OAuth JSON loaded successfully ({} client ID(s) registered)", allowedGoogleClientIds.size());
			} catch (Exception e) {
				log.warn("Could not parse Google OAuth JSON file: {}", e.getMessage());
			}
		}
	}

	public File resolveFile(String configuredPath) {
		if (configuredPath == null || configuredPath.isBlank()) {
			return null;
		}
		Path direct = Path.of(configuredPath);
		if (Files.exists(direct)) {
			return direct.toFile();
		}
		Path inSubfolder = Path.of("YolkaApi").resolve(configuredPath);
		if (Files.exists(inSubfolder)) {
			return inSubfolder.toFile();
		}
		return direct.toFile();
	}
}
