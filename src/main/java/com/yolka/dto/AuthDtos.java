package com.yolka.dto;

import com.yolka.models.UserDocument;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;

public class AuthDtos {

	public record RegisterRequest(
			@NotBlank(message = "El nombre completo es obligatorio") String fullName,
			@NotBlank(message = "El correo es obligatorio") @Email(message = "Formato de correo inválido") String email,
			@NotBlank(message = "La contraseña es obligatoria") String password,
			String confirmPassword,
			String phone,
			String deviceId,
			String deviceToken
	) {}

	public record LoginRequest(
			@NotBlank(message = "El correo es obligatorio") @Email(message = "Formato de correo inválido") String email,
			@NotBlank(message = "La contraseña es obligatoria") String password,
			String deviceId,
			String deviceToken
	) {}

	public record GoogleLoginRequest(
			String idToken,
			String authorizationCode,
			String deviceId,
			String deviceToken
	) {}

	public record RecoverPasswordRequest(
			@NotBlank(message = "El correo es obligatorio") @Email(message = "Formato de correo inválido") String email
	) {}

	public record ResetPasswordRequest(
			@NotBlank(message = "El correo es obligatorio") @Email String email,
			@NotBlank(message = "El código de recuperación es obligatorio") String code,
			@NotBlank(message = "La nueva contraseña es obligatoria") String newPassword
	) {}

	public record ConfirmAccountRequest(
			@NotBlank(message = "El token o código de confirmación es obligatorio") String token
	) {}

	public record RequestConfirmationRequest(
			@NotBlank(message = "El correo es obligatorio") @Email(message = "Formato de correo inválido") String email
	) {}

	public record LogoutRequest(
			String deviceId,
			String refreshToken
	) {}

	public record RefreshTokenRequest(
			@NotBlank(message = "El refreshToken es obligatorio") String refreshToken,
			String deviceId
	) {}

	public record UserResponse(
			String id,
			String fullName,
			String firstName,
			String lastName,
			String email,
			String phone,
			String photoUrl,
			String authProvider,
			boolean google,
			boolean confirmed,
			UserDocument.UserPreferences preferences,
			List<UserDocument.DeviceInfo> activeDevices,
			boolean hasDeviceBackup,
			Instant lastDeviceBackupAt,
			Instant createdAt
	) {
		public static UserResponse fromDocument(UserDocument doc) {
			return new UserResponse(
					doc.getId(),
					doc.getFullName(),
					doc.getFirstName(),
					doc.getLastName(),
					doc.getEmail(),
					doc.getPhone(),
					doc.getPhotoUrl(),
					doc.getAuthProvider(),
					"GOOGLE".equalsIgnoreCase(doc.getAuthProvider()) || (doc.getGoogleId() != null && !doc.getGoogleId().isBlank()),
					doc.isConfirmed(),
					doc.getPreferences(),
					doc.getActiveDevices(),
					doc.isHasDeviceBackup(),
					doc.getLastDeviceBackupAt(),
					doc.getCreatedAt()
			);
		}
	}

	public record AuthResponse(
			boolean success,
			UserResponse user,
			String token,
			String refreshToken,
			String message
	) {}
}
