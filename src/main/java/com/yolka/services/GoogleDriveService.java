package com.yolka.services;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.yolka.config.EnvsConfig;
import com.yolka.helpers.CustomError;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleDriveService {

	private final EnvsConfig envsConfig;

	private Drive driveService;

	@Getter
	private boolean configured = false;

	private String resolvedFolderId;

	public record DriveUploadResult(
			String fileId,
			String fileName,
			String webViewLink,
			String directUrl,
			long fileSize
	) {}

	@PostConstruct
	public void initialize() {
		try {
			String credPath = envsConfig.getGoogleDriveCredentialsJsonPath();
			if (credPath == null || credPath.isBlank()) {
				credPath = envsConfig.getFirebaseAdminCredentialsJsonPath();
			}

			java.io.File credentialsFile = envsConfig.resolveFile(credPath);
			if (credentialsFile == null || !credentialsFile.exists()) {
				log.warn("Google Drive credentials file not found. Drive uploads will fallback to local storage.");
				return;
			}

			try (FileInputStream is = new FileInputStream(credentialsFile)) {
				GoogleCredentials credentials = GoogleCredentials.fromStream(is)
						.createScoped(Collections.singletonList(DriveScopes.DRIVE));

				this.driveService = new Drive.Builder(
						GoogleNetHttpTransport.newTrustedTransport(),
						GsonFactory.getDefaultInstance(),
						new HttpCredentialsAdapter(credentials)
				).setApplicationName("Yolka").build();

				this.resolvedFolderId = resolveOrCreateFolder(envsConfig.getGoogleDriveFolderId());
				this.configured = true;
				log.info("Google Drive service connected successfully. Target folder ID: {}", resolvedFolderId);
			}
		} catch (Exception e) {
			log.warn("Failed to initialize Google Drive service ({}). Using local storage fallback.", e.getMessage());
			this.configured = false;
		}
	}

	private String resolveOrCreateFolder(String configuredFolderId) {
		if (configuredFolderId != null && !configuredFolderId.isBlank()) {
			return configuredFolderId.trim();
		}
		try {
			FileList result = driveService.files().list()
					.setQ("mimeType = 'application/vnd.google-apps.folder' and name = 'Yolka_Storage' and trashed = false")
					.setSpaces("drive")
					.setFields("files(id, name)")
					.execute();

			if (result.getFiles() != null && !result.getFiles().isEmpty()) {
				return result.getFiles().get(0).getId();
			}

			File folderMetadata = new File();
			folderMetadata.setName("Yolka_Storage");
			folderMetadata.setMimeType("application/vnd.google-apps.folder");

			File createdFolder = driveService.files().create(folderMetadata)
					.setFields("id")
					.execute();
			return createdFolder.getId();
		} catch (Exception e) {
			log.warn("Could not query or create default Drive folder: {}", e.getMessage());
			return null;
		}
	}

	public DriveUploadResult uploadFile(String fileName, String mimeType, InputStream inputStream, boolean makePublic) {
		if (!configured || driveService == null) {
			throw CustomError.internalServer("Google Drive no está configurado", "DRIVE_NOT_CONFIGURED");
		}

		try {
			File fileMetadata = new File();
			fileMetadata.setName(fileName);
			if (resolvedFolderId != null && !resolvedFolderId.isBlank()) {
				fileMetadata.setParents(Collections.singletonList(resolvedFolderId));
			}

			InputStreamContent mediaContent = new InputStreamContent(mimeType, inputStream);
			File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
					.setFields("id, name, webViewLink, webContentLink, size")
					.execute();

			String directUrl = "https://lh3.googleusercontent.com/d/" + uploadedFile.getId();

			if (makePublic) {
				try {
					Permission permission = new Permission()
							.setType("anyone")
							.setRole("reader");
					driveService.permissions().create(uploadedFile.getId(), permission).execute();
				} catch (Exception e) {
					log.warn("Could not set public permission on Drive file {}: {}", uploadedFile.getId(), e.getMessage());
				}
			}

			long size = uploadedFile.getSize() != null ? uploadedFile.getSize() : 0L;
			return new DriveUploadResult(
					uploadedFile.getId(),
					uploadedFile.getName(),
					uploadedFile.getWebViewLink(),
					directUrl,
					size
			);
		} catch (Exception e) {
			log.error("Error uploading file to Google Drive: {}", e.getMessage());
			throw CustomError.internalServer("Error al subir archivo a Google Drive: " + e.getMessage(), "DRIVE_UPLOAD_ERROR");
		}
	}

	public void downloadFile(String fileId, OutputStream outputStream) {
		if (!configured || driveService == null) {
			throw CustomError.internalServer("Google Drive no está configurado", "DRIVE_NOT_CONFIGURED");
		}
		try {
			driveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
		} catch (Exception e) {
			log.error("Error downloading file from Google Drive: {}", e.getMessage());
			throw CustomError.internalServer("Error al descargar archivo de Google Drive: " + e.getMessage(), "DRIVE_DOWNLOAD_ERROR");
		}
	}

	public void deleteFile(String fileId) {
		if (!configured || driveService == null || fileId == null || fileId.isBlank()) {
			return;
		}
		try {
			driveService.files().delete(fileId).execute();
		} catch (Exception e) {
			log.warn("Could not delete Drive file {}: {}", fileId, e.getMessage());
		}
	}
}
