package com.yolka.controllers;

import com.yolka.helpers.CustomError;
import com.yolka.services.BackupsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BackupsController {

	private final BackupsService backupsService;

	@PostMapping({"/backups", "/device-backups"})
	public ResponseEntity<Map<String, Object>> createBackup(
			@RequestParam(required = false) String userId,
			HttpServletRequest httpRequest
	) {
		String effectiveUserId = resolveUserId(userId, httpRequest);
		return ResponseEntity.ok(backupsService.createCloudBackup(effectiveUserId));
	}

	@GetMapping({"/backups/restore", "/device-backups"})
	public ResponseEntity<Map<String, Object>> restoreBackup(
			@RequestParam(required = false) String userId,
			HttpServletRequest httpRequest
	) {
		String effectiveUserId = resolveUserId(userId, httpRequest);
		return ResponseEntity.ok(backupsService.restoreCloudBackup(effectiveUserId));
	}

	private String resolveUserId(String queryUserId, HttpServletRequest request) {
		if (queryUserId != null && !queryUserId.isBlank()) {
			return queryUserId;
		}
		String authUserId = (String) request.getAttribute("authenticatedUserId");
		if (authUserId != null && !authUserId.isBlank()) {
			return authUserId;
		}
		throw CustomError.unauthorized("Se requiere userId o token de sesión x-token", "USER_ID_REQUIRED");
	}
}
