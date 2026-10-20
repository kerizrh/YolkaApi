package com.yolka.services;

import com.yolka.dto.AuthDtos.*;
import com.yolka.helpers.*;
import com.yolka.helpers.GoogleVerifyHelper.GoogleUserPayload;
import com.yolka.models.ProcessTokenDocument;
import com.yolka.models.UserDocument;
import com.yolka.models.UserDocument.DeviceInfo;
import com.yolka.repositories.ProcessTokenRepository;
import com.yolka.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final ProcessTokenRepository processTokenRepository;
	private final HashPasswordsHelper hashPasswordsHelper;
	private final JwtHelper jwtHelper;
	private final GoogleVerifyHelper googleVerifyHelper;
	private final MailService mailService;
	private final EmailTemplates emailTemplates;
	private final SecureRandom secureRandom = new SecureRandom();

	public AuthResponse register(RegisterRequest request) {
		if (request.confirmPassword() != null && !request.confirmPassword().isBlank()
				&& !request.password().equals(request.confirmPassword())) {
			throw CustomError.badRequest("Las contraseñas no coinciden", "PASSWORDS_DO_NOT_MATCH");
		}

		String normalizedEmail = request.email().trim().toLowerCase();
		if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
			throw CustomError.badRequest("Ya existe un usuario con el correo " + normalizedEmail, "USER_ALREADY_EXISTS");
		}

		String[] nameParts = request.fullName().trim().split("\\s+", 2);
		String firstName = nameParts[0];
		String lastName = nameParts.length > 1 ? nameParts[1] : "";

		UserDocument newUser = UserDocument.builder()
				.fullName(request.fullName().trim())
				.firstName(firstName)
				.lastName(lastName)
				.email(normalizedEmail)
				.password(hashPasswordsHelper.hash(request.password()))
				.phone(request.phone() != null ? request.phone().trim() : "")
				.authProvider("LOCAL")
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();

		updateUserDevice(newUser, request.deviceId(), request.deviceToken());
		newUser = userRepository.save(newUser);

		mailService.sendEmail(
				newUser.getEmail(),
				"¡Bienvenido a Yolka!",
				emailTemplates.welcomeTemplate(newUser.getFirstName())
		);

		return issueTokensAndBuildResponse(newUser, request.deviceId(), "Usuario registrado exitosamente");
	}

	public AuthResponse login(LoginRequest request) {
		String normalizedEmail = request.email().trim().toLowerCase();
		UserDocument user = userRepository.findByEmailIgnoreCase(normalizedEmail)
				.orElseThrow(() -> CustomError.unauthorized("Credenciales inválidas", "INVALID_CREDENTIALS"));

		if (user.getPassword() == null || !hashPasswordsHelper.compare(request.password(), user.getPassword())) {
			throw CustomError.unauthorized("Credenciales inválidas", "INVALID_CREDENTIALS");
		}

		updateUserDevice(user, request.deviceId(), request.deviceToken());
		user.setUpdatedAt(Instant.now());
		user = userRepository.save(user);

		return issueTokensAndBuildResponse(user, request.deviceId(), "Inicio de sesión exitoso");
	}

	public AuthResponse googleSignIn(GoogleLoginRequest request) {
		GoogleUserPayload googleUser = googleVerifyHelper.verifyGoogleSignIn(
				request.idToken(),
				request.authorizationCode()
		);

		if (googleUser.email() == null || googleUser.email().isBlank()) {
			throw CustomError.badRequest("La cuenta de Google no proporcionó un correo válido", "GOOGLE_EMAIL_MISSING");
		}

		String normalizedEmail = googleUser.email().trim().toLowerCase();
		UserDocument user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);

		if (user == null) {
			String fullName = (googleUser.name() != null && !googleUser.name().isBlank())
					? googleUser.name()
					: (googleUser.givenName() + " " + googleUser.familyName()).trim();
			if (fullName.isBlank()) {
				fullName = normalizedEmail.split("@")[0];
			}

			user = UserDocument.builder()
					.fullName(fullName)
					.firstName(googleUser.givenName() != null ? googleUser.givenName() : fullName)
					.lastName(googleUser.familyName() != null ? googleUser.familyName() : "")
					.email(normalizedEmail)
					.authProvider("GOOGLE")
					.googleId(googleUser.sub())
					.photoUrl(googleUser.picture())
					.createdAt(Instant.now())
					.updatedAt(Instant.now())
					.build();
		} else {
			user.setGoogleId(googleUser.sub());
			if (user.getPhotoUrl() == null || user.getPhotoUrl().isBlank()) {
				user.setPhotoUrl(googleUser.picture());
			}
			user.setUpdatedAt(Instant.now());
		}

		updateUserDevice(user, request.deviceId(), request.deviceToken());
		user = userRepository.save(user);

		return issueTokensAndBuildResponse(user, request.deviceId(), "Autenticación con Google exitosa");
	}

	public Map<String, Object> recoverPassword(RecoverPasswordRequest request) {
		String normalizedEmail = request.email().trim().toLowerCase();
		UserDocument user = userRepository.findByEmailIgnoreCase(normalizedEmail)
				.orElseThrow(() -> CustomError.notFound("No existe una cuenta registrada con ese correo", "USER_NOT_FOUND"));

		String resetCode = String.format("%06d", secureRandom.nextInt(1_000_000));
		ProcessTokenDocument resetTokenDoc = ProcessTokenDocument.builder()
				.token(resetCode)
				.type("passwordReset")
				.userId(user.getId())
				.deviceId("recovery")
				.expires(Instant.now().plusSeconds(15 * 60))
				.build();
		processTokenRepository.save(resetTokenDoc);

		mailService.sendEmail(
				user.getEmail(),
				"Código de recuperación de contraseña — Yolka",
				emailTemplates.resetPasswordTemplate(user.getFullName(), resetCode)
		);

		return Map.of(
				"success", true,
				"message", "Se ha enviado un código de recuperación a tu correo electrónico"
		);
	}

	public Map<String, Object> resetPassword(ResetPasswordRequest request) {
		String normalizedEmail = request.email().trim().toLowerCase();
		UserDocument user = userRepository.findByEmailIgnoreCase(normalizedEmail)
				.orElseThrow(() -> CustomError.notFound("Usuario no encontrado", "USER_NOT_FOUND"));

		ProcessTokenDocument tokenDoc = processTokenRepository.findByTokenAndType(request.code().trim(), "passwordReset")
				.orElseThrow(() -> CustomError.badRequest("Código de recuperación inválido", "INVALID_RESET_CODE"));

		if (!user.getId().equals(tokenDoc.getUserId()) || tokenDoc.getExpires().isBefore(Instant.now())) {
			throw CustomError.badRequest("El código de recuperación ha expirado", "EXPIRED_RESET_CODE");
		}

		user.setPassword(hashPasswordsHelper.hash(request.newPassword()));
		user.setUpdatedAt(Instant.now());
		userRepository.save(user);
		processTokenRepository.delete(tokenDoc);

		return Map.of(
				"success", true,
				"message", "Contraseña actualizada correctamente"
		);
	}

	private void updateUserDevice(UserDocument user, String deviceId, String deviceToken) {
		String effectiveDeviceId = (deviceId != null && !deviceId.isBlank()) ? deviceId.trim() : "default-mobile";
		String effectiveToken = (deviceToken != null) ? deviceToken.trim() : "";

		if (!effectiveToken.isBlank()) {
			user.setFcmToken(effectiveToken);
		}

		if (user.getDevices() == null) {
			user.setDevices(new ArrayList<>());
		}
		if (user.getActiveDevices() == null) {
			user.setActiveDevices(new ArrayList<>());
		}

		user.getDevices().removeIf(d -> effectiveDeviceId.equals(d.getDeviceId()));
		user.getDevices().add(new DeviceInfo(effectiveDeviceId, effectiveToken));

		user.getActiveDevices().removeIf(d -> effectiveDeviceId.equals(d.getDeviceId()));
		user.getActiveDevices().add(new DeviceInfo(effectiveDeviceId, effectiveToken));
	}

	private AuthResponse issueTokensAndBuildResponse(UserDocument user, String deviceId, String message) {
		String effectiveDeviceId = (deviceId != null && !deviceId.isBlank()) ? deviceId.trim() : "default-mobile";
		String accessToken = jwtHelper.generateAccessToken(user.getId(), user.getEmail(), effectiveDeviceId);
		String refreshToken = jwtHelper.generateRefreshToken(user.getId(), user.getEmail(), effectiveDeviceId);

		processTokenRepository.save(ProcessTokenDocument.builder()
				.token(accessToken)
				.type("accessToken")
				.userId(user.getId())
				.deviceId(effectiveDeviceId)
				.expires(Instant.now().plusSeconds(24 * 60 * 60))
				.build());

		processTokenRepository.save(ProcessTokenDocument.builder()
				.token(refreshToken)
				.type("refreshToken")
				.userId(user.getId())
				.deviceId(effectiveDeviceId)
				.expires(Instant.now().plusSeconds(180L * 24 * 60 * 60))
				.build());

		return new AuthResponse(
				true,
				UserResponse.fromDocument(user),
				accessToken,
				refreshToken,
				message
		);
	}
}
