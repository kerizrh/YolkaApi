package com.yolka.controllers;

import com.yolka.dto.AuthDtos.UserResponse;
import com.yolka.dto.UserDtos.*;
import com.yolka.helpers.CustomError;
import com.yolka.helpers.HashPasswordsHelper;
import com.yolka.models.UserDocument;
import com.yolka.models.UserDocument.DeviceInfo;
import com.yolka.repositories.*;
import com.yolka.services.BackupsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/users", "/api/users"})
@RequiredArgsConstructor
public class UsersController {

	private final UserRepository userRepository;
	private final PetRepository petRepository;
	private final NotificationRepository notificationRepository;
	private final BackupRepository backupRepository;
	private final ProcessTokenRepository processTokenRepository;
	private final HashPasswordsHelper hashPasswordsHelper;
	private final BackupsService backupsService;

	@GetMapping("/{id}")
	public ResponseEntity<Map<String, Object>> getUserProfile(@PathVariable String id) {
		UserDocument user = userRepository.findByIdAndDeletedFalse(id)
				.orElseThrow(() -> CustomError.notFound("Usuario no encontrado", "USER_NOT_FOUND"));
		return ResponseEntity.ok(Map.of("success", true, "user", UserResponse.fromDocument(user)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Map<String, Object>> updateUserProfile(
			@PathVariable String id,
			@RequestBody UpdateProfileRequest request
	) {
		UserDocument user = userRepository.findByIdAndDeletedFalse(id)
				.orElseThrow(() -> CustomError.notFound("Usuario no encontrado", "USER_NOT_FOUND"));

		if (request.fullName() != null && !request.fullName().isBlank()) {
			user.setFullName(request.fullName().trim());
			String[] parts = request.fullName().trim().split("\\s+", 2);
			user.setFirstName(parts[0]);
			user.setLastName(parts.length > 1 ? parts[1] : "");
		}
		if (request.phone() != null) user.setPhone(request.phone().trim());
		if (request.photoUrl() != null) user.setPhotoUrl(request.photoUrl().trim());
		if (request.preferences() != null) user.setPreferences(request.preferences());

		user.setUpdatedAt(Instant.now());
		user = userRepository.save(user);

		return ResponseEntity.ok(Map.of("success", true, "user", UserResponse.fromDocument(user)));
	}

	@PutMapping("/{id}/password")
	public ResponseEntity<Map<String, Object>> changePassword(
			@PathVariable String id,
			@Valid @RequestBody ChangePasswordRequest request
	) {
		UserDocument user = userRepository.findByIdAndDeletedFalse(id)
				.orElseThrow(() -> CustomError.notFound("Usuario no encontrado", "USER_NOT_FOUND"));

		if (user.getPassword() != null && !user.getPassword().isBlank()
				&& !hashPasswordsHelper.compare(request.currentPassword(), user.getPassword())) {
			throw CustomError.badRequest("La contraseña actual es incorrecta", "INVALID_CURRENT_PASSWORD");
		}

		user.setPassword(hashPasswordsHelper.hash(request.newPassword()));
		user.setUpdatedAt(Instant.now());
		userRepository.save(user);

		return ResponseEntity.ok(Map.of("success", true, "message", "Contraseña actualizada exitosamente"));
	}

	@PutMapping("/{id}/fcm-token")
	public ResponseEntity<Map<String, Object>> updateFcmToken(
			@PathVariable String id,
			@Valid @RequestBody FcmTokenRequest request
	) {
		UserDocument user = userRepository.findByIdAndDeletedFalse(id)
				.orElseThrow(() -> CustomError.notFound("Usuario no encontrado", "USER_NOT_FOUND"));

		String deviceId = (request.deviceId() != null && !request.deviceId().isBlank())
				? request.deviceId().trim()
				: "default-mobile";

		user.setFcmToken(request.fcmToken().trim());
		user.getDevices().removeIf(d -> deviceId.equals(d.getDeviceId()));
		user.getDevices().add(new DeviceInfo(deviceId, request.fcmToken().trim()));
		user.getActiveDevices().removeIf(d -> deviceId.equals(d.getDeviceId()));
		user.getActiveDevices().add(new DeviceInfo(deviceId, request.fcmToken().trim()));
		user.setUpdatedAt(Instant.now());
		userRepository.save(user);

		return ResponseEntity.ok(Map.of("success", true, "message", "Token FCM sincronizado"));
	}

	@GetMapping("/{id}/backup")
	public ResponseEntity<Map<String, Object>> exportBackup(@PathVariable String id) {
		return ResponseEntity.ok(backupsService.createCloudBackup(id));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Map<String, Object>> deleteAccount(@PathVariable String id) {
		UserDocument user = userRepository.findByIdAndDeletedFalse(id)
				.orElseThrow(() -> CustomError.notFound("Usuario no encontrado", "USER_NOT_FOUND"));

		Instant now = Instant.now();

		petRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(id).forEach(pet -> {
			pet.setDeleted(true);
			pet.setDeletedAt(now);
			pet.setUpdatedAt(now);
			petRepository.save(pet);
		});

		notificationRepository.findByUserIdAndDeletedFalseOrderByNextNotificationDateAsc(id).forEach(notif -> {
			notif.setActive(false);
			notif.setDeleted(true);
			notif.setDeletedAt(now);
			notificationRepository.save(notif);
		});

		backupRepository.findByUserIdAndDeletedFalse(id).forEach(backup -> {
			backup.setDeleted(true);
			backup.setDeletedAt(now);
			backup.setUpdatedAt(now);
			backupRepository.save(backup);
		});

		processTokenRepository.findByUserIdAndDeletedFalse(id).forEach(token -> {
			token.setDeleted(true);
			token.setDeletedAt(now);
			processTokenRepository.save(token);
		});

		user.setDeleted(true);
		user.setDeletedAt(now);
		user.setUpdatedAt(now);
		user.getActiveDevices().clear();
		userRepository.save(user);

		return ResponseEntity.ok(Map.of("success", true, "message", "Cuenta y datos asociados dados de baja (Soft Delete) correctamente"));
	}
}
