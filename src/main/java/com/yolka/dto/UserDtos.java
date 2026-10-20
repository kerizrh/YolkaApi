package com.yolka.dto;

import com.yolka.models.UserDocument;
import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

public class UserDtos {

	public record UpdateProfileRequest(
			String fullName,
			String phone,
			String photoUrl,
			UserDocument.UserPreferences preferences
	) {}

	public record ChangePasswordRequest(
			@NotBlank(message = "La contraseña actual es obligatoria") String currentPassword,
			@NotBlank(message = "La nueva contraseña es obligatoria") String newPassword
	) {}

	public record FcmTokenRequest(
			String deviceId,
			@NotBlank(message = "El fcmToken es obligatorio") String fcmToken
	) {}

	public record CreateNotificationRequest(
			String userId,
			String petId,
			String petName,
			@NotBlank(message = "La categoría es obligatoria") String category,
			@NotBlank(message = "El título es obligatorio") String title,
			String body,
			@NotBlank(message = "La fecha/hora programada es obligatoria") String scheduledTime,
			String timeZone,
			String frequencyUnit,
			Integer frequencyInterval,
			List<String> daysOfWeek
	) {}

	public record SendPushRequest(
			String userId,
			String fcmToken,
			@NotBlank(message = "El título es obligatorio") String title,
			@NotBlank(message = "El mensaje es obligatorio") String body,
			Map<String, String> data
	) {}
}
