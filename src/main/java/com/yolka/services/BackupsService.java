package com.yolka.services;

import com.yolka.config.EnvsConfig;
import com.yolka.helpers.CryptoHelper;
import com.yolka.helpers.CryptoHelper.EncryptedPayload;
import com.yolka.helpers.CustomError;
import com.yolka.helpers.UuidGeneratorHelper;
import com.yolka.models.BackupDocument;
import com.yolka.models.NotificationDocument;
import com.yolka.models.PetDocument;
import com.yolka.models.UserDocument;
import com.yolka.repositories.BackupRepository;
import com.yolka.repositories.NotificationRepository;
import com.yolka.repositories.PetRepository;
import com.yolka.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class BackupsService {

	private final UserRepository userRepository;
	private final PetRepository petRepository;
	private final NotificationRepository notificationRepository;
	private final BackupRepository backupRepository;
	private final CryptoHelper cryptoHelper;
	private final UuidGeneratorHelper uuidGeneratorHelper;
	private final EnvsConfig envsConfig;
	private final GoogleDriveService googleDriveService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public Map<String, Object> createCloudBackup(String userId) {
		UserDocument user = userRepository.findByIdAndDeletedFalse(userId)
				.orElseThrow(() -> CustomError.notFound("Usuario no encontrado", "USER_NOT_FOUND"));

		List<PetDocument> pets = petRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId);
		List<NotificationDocument> notifications = notificationRepository.findByUserIdAndDeletedFalseOrderByNextNotificationDateAsc(userId);

		Map<String, Object> backupPayload = new LinkedHashMap<>();
		backupPayload.put("userId", user.getId());
		backupPayload.put("email", user.getEmail());
		backupPayload.put("preferences", user.getPreferences());
		backupPayload.put("pets", pets);
		backupPayload.put("notifications", notifications);
		backupPayload.put("exportedAt", Instant.now().toString());

		try {
			byte[] jsonBytes = objectMapper.writeValueAsBytes(backupPayload);
			byte[] gzippedBytes = gzipCompress(jsonBytes);
			EncryptedPayload encrypted = cryptoHelper.encryptBuffer(gzippedBytes);
			String fileContent = encrypted.toCombinedString();

			Path dir = Path.of(envsConfig.getBackupStoragePath());
			Files.createDirectories(dir);

			String fileName = userId + "-" + System.currentTimeMillis() + ".v2.ylk";
			Path filePath = dir.resolve(fileName);
			Files.writeString(filePath, fileContent, StandardCharsets.UTF_8);

			backupRepository.findFirstByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId).ifPresent(prev -> {
				try {
					Files.deleteIfExists(dir.resolve(prev.getFileName()));
				} catch (Exception ignored) {
				}
				if (prev.getDriveFileId() != null && googleDriveService.isConfigured()) {
					googleDriveService.deleteFile(prev.getDriveFileId());
				}
			});

			String driveFileId = null;
			String driveWebViewLink = null;
			if (googleDriveService.isConfigured()) {
				try {
					var driveResult = googleDriveService.uploadFile(
							fileName,
							"application/octet-stream",
							new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8)),
							false
					);
					driveFileId = driveResult.fileId();
					driveWebViewLink = driveResult.webViewLink();
					log.info("Encrypted backup uploaded to Google Drive. FileId: {}", driveFileId);
				} catch (Exception ex) {
					log.warn("Failed to upload backup to Google Drive, preserved locally: {}", ex.getMessage());
				}
			}

			Instant now = Instant.now();
			String newBackupId = uuidGeneratorHelper.generate();
			BackupDocument backupDoc = backupRepository.findFirstByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)
					.orElse(BackupDocument.builder()
							.id(newBackupId)
							.uuid(newBackupId)
							.userId(userId)
							.deleted(false)
							.createdAt(now)
							.build());
			backupDoc.setFileName(fileName);
			backupDoc.setFileSize(Files.size(filePath));
			backupDoc.setType("cloud_encrypted_v2");
			if (driveFileId != null) {
				backupDoc.setDriveFileId(driveFileId);
				backupDoc.setDriveWebViewLink(driveWebViewLink);
			}
			backupDoc.setUpdatedAt(now);
			backupDoc = backupRepository.save(backupDoc);

			user.setHasDeviceBackup(true);
			user.setLastDeviceBackupAt(now);
			userRepository.save(user);

			return Map.of(
					"success", true,
					"message", "Respaldo cifrado creado exitosamente",
					"backupInfo", backupDoc
			);
		} catch (Exception e) {
			log.error("Error creating encrypted backup: {}", e.getMessage());
			throw CustomError.internalServer("Error al generar el respaldo: " + e.getMessage(), "BACKUP_CREATION_ERROR");
		}
	}

	public Map<String, Object> restoreCloudBackup(String userId) {
		UserDocument user = userRepository.findByIdAndDeletedFalse(userId)
				.orElseThrow(() -> CustomError.notFound("Usuario no encontrado", "USER_NOT_FOUND"));

		BackupDocument backupDoc = backupRepository.findFirstByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId)
				.orElseThrow(() -> CustomError.notFound("No existe un respaldo previo para este usuario", "NO_BACKUP_FOUND"));

		try {
			Path filePath = Path.of(envsConfig.getBackupStoragePath()).resolve(backupDoc.getFileName());
			String encryptedContent;
			if (Files.exists(filePath)) {
				encryptedContent = Files.readString(filePath, StandardCharsets.UTF_8);
			} else if (backupDoc.getDriveFileId() != null && googleDriveService.isConfigured()) {
				log.info("Local backup file not found, downloading from Google Drive ({})", backupDoc.getDriveFileId());
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				googleDriveService.downloadFile(backupDoc.getDriveFileId(), baos);
				encryptedContent = baos.toString(StandardCharsets.UTF_8);
			} else {
				throw CustomError.notFound("Archivo físico de respaldo no encontrado", "BACKUP_FILE_MISSING");
			}
			byte[] decryptedGzip = cryptoHelper.decryptBuffer(encryptedContent);
			byte[] unzippedJson = gzipDecompress(decryptedGzip);

			Map<String, Object> payload = objectMapper.readValue(unzippedJson, new TypeReference<>() {});
			List<PetDocument> restoredPets = objectMapper.convertValue(
					payload.getOrDefault("pets", List.of()),
					new TypeReference<List<PetDocument>>() {}
			);

			if (!restoredPets.isEmpty()) {
				Instant now = Instant.now();
				petRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId).forEach(existing -> {
					existing.setDeleted(true);
					existing.setDeletedAt(now);
					petRepository.save(existing);
				});
				restoredPets.forEach(p -> {
					p.setDeleted(false);
					p.setDeletedAt(null);
				});
				petRepository.saveAll(restoredPets);
			}

			return Map.of(
					"success", true,
					"message", "Respaldo restaurado exitosamente",
					"backupInfo", backupDoc,
					"data", payload
			);
		} catch (CustomError ce) {
			throw ce;
		} catch (Exception e) {
			log.error("Error restoring backup: {}", e.getMessage());
			throw CustomError.internalServer("Error al restaurar el respaldo: " + e.getMessage(), "BACKUP_RESTORE_ERROR");
		}
	}

	private byte[] gzipCompress(byte[] data) throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try (GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
			gzip.write(data);
		}
		return bos.toByteArray();
	}

	private byte[] gzipDecompress(byte[] compressed) throws Exception {
		try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed));
			 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			gis.transferTo(bos);
			return bos.toByteArray();
		}
	}
}
