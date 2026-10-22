package com.yolka.controllers;

import com.yolka.helpers.CustomError;
import com.yolka.services.GoogleDriveService;
import com.yolka.services.GoogleDriveService.DriveUploadResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping({"/api/v1/storage", "/api/storage"})
@RequiredArgsConstructor
public class StorageController {

	private final GoogleDriveService googleDriveService;
	private final Path localUploadDir = Path.of("uploads");

	@PostMapping("/upload")
	public ResponseEntity<Map<String, Object>> uploadFile(
			@RequestParam("file") MultipartFile file,
			@RequestParam(value = "folder", required = false) String folder,
			HttpServletRequest request
	) {
		if (file.isEmpty()) {
			throw CustomError.badRequest("El archivo proporcionado está vacío", "FILE_EMPTY");
		}

		String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image.jpg";
		String extension = "";
		int dotIdx = originalName.lastIndexOf('.');
		if (dotIdx >= 0) {
			extension = originalName.substring(dotIdx);
		}
		String cleanName = UUID.randomUUID() + extension;
		String contentType = file.getContentType() != null ? file.getContentType() : "image/jpeg";

		// 1. Intentar subir a Google Drive si está configurado
		if (googleDriveService.isConfigured()) {
			try (InputStream is = file.getInputStream()) {
				DriveUploadResult result = googleDriveService.uploadFile(cleanName, contentType, is, true);
				return ResponseEntity.ok(Map.of(
						"success", true,
						"url", result.directUrl(),
						"webViewLink", result.webViewLink() != null ? result.webViewLink() : "",
						"fileId", result.fileId(),
						"fileName", result.fileName(),
						"fileSize", result.fileSize(),
						"storageProvider", "GOOGLE_DRIVE"
				));
			} catch (Exception ex) {
				log.warn("Fallo subida a Google Drive ({}), usando respaldo local", ex.getMessage());
			}
		}

		// 2. Fallback de almacenamiento local
		try {
			Files.createDirectories(localUploadDir);
			Path destination = localUploadDir.resolve(cleanName);
			Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

			String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
					.path("/api/v1/storage/files/")
					.path(cleanName)
					.toUriString();

			return ResponseEntity.ok(Map.of(
					"success", true,
					"url", fileDownloadUri,
					"fileName", cleanName,
					"fileSize", file.getSize(),
					"storageProvider", "LOCAL"
			));
		} catch (Exception e) {
			log.error("Error guardando archivo localmente: {}", e.getMessage());
			throw CustomError.internalServer("Error al almacenar el archivo: " + e.getMessage(), "FILE_UPLOAD_ERROR");
		}
	}

	@GetMapping("/files/{filename:.+}")
	public ResponseEntity<Resource> serveLocalFile(@PathVariable String filename) {
		try {
			Path file = localUploadDir.resolve(filename);
			Resource resource = new UrlResource(file.toUri());

			if (resource.exists() || resource.isReadable()) {
				String contentType = Files.probeContentType(file);
				if (contentType == null) {
					contentType = "application/octet-stream";
				}
				return ResponseEntity.ok()
						.contentType(MediaType.parseMediaType(contentType))
						.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
						.body(resource);
			} else {
				throw CustomError.notFound("Archivo no encontrado", "FILE_NOT_FOUND");
			}
		} catch (Exception e) {
			throw CustomError.notFound("Archivo no encontrado", "FILE_NOT_FOUND");
		}
	}
}
