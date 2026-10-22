package com.yolka.services;

import com.yolka.config.EnvsConfig;
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
	private final UuidGeneratorHelper uuidGeneratorHelper;
	private final MailService mailService;
	private final EmailTemplates emailTemplates;
	private final EnvsConfig envsConfig;
	private final SecureRandom secureRandom = new SecureRandom();

	public AuthResponse register(RegisterRequest request) {
		if (request.confirmPassword() != null && !request.confirmPassword().isBlank()
				&& !request.password().equals(request.confirmPassword())) {
			throw CustomError.badRequest("Las contraseñas no coinciden", "PASSWORDS_DO_NOT_MATCH");
		}

		String normalizedEmail = request.email().trim().toLowerCase();
		String[] nameParts = request.fullName().trim().split("\\s+", 2);
		String firstName = nameParts[0];
		String lastName = nameParts.length > 1 ? nameParts[1] : "";

		UserDocument existingUser = userRepository.findFirstByEmailIgnoreCase(normalizedEmail).orElse(null);
		if (existingUser != null) {
			if (!existingUser.isDeleted()) {
				throw CustomError.badRequest("Ya existe un usuario con el correo " + normalizedEmail, "USER_ALREADY_EXISTS");
			}

			// Reactivar usuario previamente eliminado (Soft Delete) sin crear otro documento
			existingUser.setFullName(request.fullName().trim());
			existingUser.setFirstName(firstName);
			existingUser.setLastName(lastName);
			existingUser.setPassword(hashPasswordsHelper.hash(request.password()));
			if (request.phone() != null && !request.phone().isBlank()) {
				existingUser.setPhone(request.phone().trim());
			}
			existingUser.setConfirmed(false);
			existingUser.setDeleted(false);
			existingUser.setDeletedAt(null);
			existingUser.setUpdatedAt(Instant.now());

			updateUserDevice(existingUser, request.deviceId(), request.deviceToken());
			existingUser = userRepository.save(existingUser);

			sendConfirmationEmail(existingUser, true);

			return issueTokensAndBuildResponse(
					existingUser,
					request.deviceId(),
					"Cuenta reactivada exitosamente. Hemos enviado un correo para confirmar la reactivación de tu cuenta."
			);
		}

		String uid = uuidGeneratorHelper.generate();

		UserDocument newUser = UserDocument.builder()
				.id(uid)
				.uuid(uid)
				.fullName(request.fullName().trim())
				.firstName(firstName)
				.lastName(lastName)
				.email(normalizedEmail)
				.password(hashPasswordsHelper.hash(request.password()))
				.phone(request.phone() != null ? request.phone().trim() : "")
				.authProvider("LOCAL")
				.confirmed(false)
				.deleted(false)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();

		updateUserDevice(newUser, request.deviceId(), request.deviceToken());
		newUser = userRepository.save(newUser);

		sendConfirmationEmail(newUser, false);

		return new AuthResponse(
				true,
				UserResponse.fromDocument(newUser),
				null,
				null,
				"Usuario registrado exitosamente. Hemos enviado un código de confirmación a tu correo electrónico."
		);
	}

	private void sendConfirmationEmail(UserDocument user) {
		sendConfirmationEmail(user, false);
	}

	private void sendConfirmationEmail(UserDocument user, boolean isReactivation) {
		String confirmToken = String.format("%06d", secureRandom.nextInt(1_000_000));
		ProcessTokenDocument tokenDoc = ProcessTokenDocument.builder()
				.id(uuidGeneratorHelper.generate())
				.token(confirmToken)
				.type("accountConfirmation")
				.userId(user.getId())
				.deviceId("email-confirm")
				.expires(Instant.now().plusSeconds(10 * 60))
				.deleted(false)
				.build();
		processTokenRepository.save(tokenDoc);

		String baseUrl = envsConfig.getBaseUrl() != null ? envsConfig.getBaseUrl().trim().replaceAll("/+$", "") : "";
		String confirmationUrl = baseUrl + "/auth/confirm-account?token=" + confirmToken;

		String subject = isReactivation
				? "Reactivación de tu cuenta en Yolka 🐾"
				: "Confirma tu cuenta en Yolka 🐾";
		String htmlBody = isReactivation
				? emailTemplates.reactivateAccountTemplate(user.getFirstName(), confirmationUrl, confirmToken)
				: emailTemplates.confirmAccountTemplate(user.getFirstName(), confirmationUrl, confirmToken);

		mailService.sendEmail(user.getEmail(), subject, htmlBody);
	}

	public String confirmAccountByLink(String token) {
		UserDocument user = verifyAndConsumeConfirmationToken(token);
		return emailTemplates.confirmedPageHtml(user.getFirstName());
	}

	public Map<String, Object> confirmAccountByCode(ConfirmAccountRequest request) {
		UserDocument user = verifyAndConsumeConfirmationToken(request.token());
		return Map.of(
				"success", true,
				"message", "Cuenta confirmada exitosamente",
				"user", UserResponse.fromDocument(user)
		);
	}

	public Map<String, Object> requestAccountConfirmation(RequestConfirmationRequest request) {
		String normalizedEmail = request.email().trim().toLowerCase();
		UserDocument user = userRepository.findByEmailIgnoreCaseAndDeletedFalse(normalizedEmail)
				.orElseThrow(() -> CustomError.notFound("Usuario no encontrado", "USER_NOT_FOUND"));

		if (user.isConfirmed()) {
			throw CustomError.badRequest("La cuenta ya se encuentra confirmada", "ACCOUNT_ALREADY_CONFIRMED");
		}

		sendConfirmationEmail(user);
		return Map.of(
				"success", true,
				"message", "Se ha enviado un nuevo enlace y código de confirmación a tu correo electrónico"
		);
	}

	private UserDocument verifyAndConsumeConfirmationToken(String token) {
		if (token == null || token.isBlank()) {
			throw CustomError.badRequest("El token de confirmación es obligatorio", "CONFIRMATION_TOKEN_REQUIRED");
		}

		ProcessTokenDocument tokenDoc = processTokenRepository.findByTokenAndTypeAndDeletedFalse(token.trim(), "accountConfirmation")
				.orElseThrow(() -> CustomError.badRequest("Código o enlace de confirmación inválido", "INVALID_CONFIRMATION_TOKEN"));

		if (tokenDoc.getExpires().isBefore(Instant.now())) {
			throw CustomError.badRequest("El enlace o código de confirmación ha expirado. Solicita uno nuevo.", "EXPIRED_CONFIRMATION_TOKEN");
		}

		UserDocument user = userRepository.findByIdAndDeletedFalse(tokenDoc.getUserId())
				.orElseThrow(() -> CustomError.notFound("Usuario no encontrado", "USER_NOT_FOUND"));

		user.setConfirmed(true);
		user.setUpdatedAt(Instant.now());
		user = userRepository.save(user);

		tokenDoc.setDeleted(true);
		tokenDoc.setDeletedAt(Instant.now());
		processTokenRepository.save(tokenDoc);

		return user;
	}

	public AuthResponse login(LoginRequest request) {
		String normalizedEmail = request.email().trim().toLowerCase();
		UserDocument user = userRepository.findByEmailIgnoreCaseAndDeletedFalse(normalizedEmail)
				.orElseThrow(() -> CustomError.unauthorized("Credenciales inválidas", "INVALID_CREDENTIALS"));

		if (user.getPassword() == null || !hashPasswordsHelper.compare(request.password(), user.getPassword())) {
			throw CustomError.unauthorized("Credenciales inválidas", "INVALID_CREDENTIALS");
		}

		if (!user.isConfirmed()) {
			throw CustomError.forbidden("Debes confirmar tu correo electrónico antes de ingresar a Yolka", "ACCOUNT_NOT_CONFIRMED");
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
		UserDocument user = userRepository.findFirstByEmailIgnoreCase(normalizedEmail).orElse(null);
		boolean wasDeleted = false;

		if (user == null) {
			String fullName = (googleUser.name() != null && !googleUser.name().isBlank())
					? googleUser.name()
					: (googleUser.givenName() + " " + googleUser.familyName()).trim();
			if (fullName.isBlank()) {
				fullName = normalizedEmail.split("@")[0];
			}
			String uid = uuidGeneratorHelper.generate();

			user = UserDocument.builder()
					.id(uid)
					.uuid(uid)
					.fullName(fullName)
					.firstName(googleUser.givenName() != null ? googleUser.givenName() : fullName)
					.lastName(googleUser.familyName() != null ? googleUser.familyName() : "")
					.email(normalizedEmail)
					.authProvider("GOOGLE")
					.googleId(googleUser.sub())
					.photoUrl(googleUser.picture())
					.confirmed(true)
					.deleted(false)
					.createdAt(Instant.now())
					.updatedAt(Instant.now())
					.build();
		} else {
			wasDeleted = user.isDeleted();
			user.setGoogleId(googleUser.sub());
			user.setDeleted(false);
			user.setDeletedAt(null);
			user.setConfirmed(!wasDeleted);
			if (user.getPhotoUrl() == null || user.getPhotoUrl().isBlank()) {
				user.setPhotoUrl(googleUser.picture());
			}
			user.setUpdatedAt(Instant.now());
		}

		updateUserDevice(user, request.deviceId(), request.deviceToken());
		user = userRepository.save(user);

		if (wasDeleted) {
			sendConfirmationEmail(user, true);
		}

		return issueTokensAndBuildResponse(
				user,
				request.deviceId(),
				wasDeleted
						? "Cuenta reactivada con Google exitosamente. Hemos enviado un correo para confirmar la reactivación."
						: "Autenticación con Google exitosa"
		);
	}

	public Map<String, Object> recoverPassword(RecoverPasswordRequest request) {
		String normalizedEmail = request.email().trim().toLowerCase();
		UserDocument user = userRepository.findByEmailIgnoreCaseAndDeletedFalse(normalizedEmail)
				.orElseThrow(() -> CustomError.notFound("No existe una cuenta registrada con ese correo", "USER_NOT_FOUND"));

		String resetCode = String.format("%06d", secureRandom.nextInt(1_000_000));
		ProcessTokenDocument resetTokenDoc = ProcessTokenDocument.builder()
				.id(uuidGeneratorHelper.generate())
				.token(resetCode)
				.type("passwordReset")
				.userId(user.getId())
				.deviceId("recovery")
				.expires(Instant.now().plusSeconds(15 * 60))
				.deleted(false)
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
		UserDocument user = userRepository.findByEmailIgnoreCaseAndDeletedFalse(normalizedEmail)
				.orElseThrow(() -> CustomError.notFound("Usuario no encontrado", "USER_NOT_FOUND"));

		ProcessTokenDocument tokenDoc = processTokenRepository.findByTokenAndTypeAndDeletedFalse(request.code().trim(), "passwordReset")
				.orElseThrow(() -> CustomError.badRequest("Código de recuperación inválido", "INVALID_RESET_CODE"));

		if (!user.getId().equals(tokenDoc.getUserId()) || tokenDoc.getExpires().isBefore(Instant.now())) {
			throw CustomError.badRequest("El código de recuperación ha expirado", "EXPIRED_RESET_CODE");
		}

		user.setPassword(hashPasswordsHelper.hash(request.newPassword()));
		user.setUpdatedAt(Instant.now());
		userRepository.save(user);

		tokenDoc.setDeleted(true);
		tokenDoc.setDeletedAt(Instant.now());
		processTokenRepository.save(tokenDoc);

		return Map.of(
				"success", true,
				"message", "Contraseña actualizada correctamente"
		);
	}

	public Map<String, Object> logout(String accessToken, LogoutRequest request) {
		Instant now = Instant.now();
		String userId = null;
		String deviceId = (request != null && request.deviceId() != null && !request.deviceId().isBlank())
				? request.deviceId().trim()
				: null;

		if (accessToken != null && !accessToken.isBlank()) {
			try {
				var claims = jwtHelper.validateToken(accessToken.trim());
				userId = claims.getSubject();
				if (deviceId == null) {
					deviceId = claims.get("deviceId", String.class);
				}
			} catch (Exception ignored) {
			}

			processTokenRepository.findByTokenAndTypeAndDeletedFalse(accessToken.trim(), "accessToken")
					.ifPresent(tokenDoc -> {
						tokenDoc.setDeleted(true);
						tokenDoc.setDeletedAt(now);
						processTokenRepository.save(tokenDoc);
					});
		}

		if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
			processTokenRepository.findByTokenAndTypeAndDeletedFalse(request.refreshToken().trim(), "refreshToken")
					.ifPresent(tokenDoc -> {
						tokenDoc.setDeleted(true);
						tokenDoc.setDeletedAt(now);
						processTokenRepository.save(tokenDoc);
					});
		}

		if (userId != null && deviceId != null) {
			final String targetDeviceId = deviceId;
			processTokenRepository.findByUserIdAndDeviceIdAndDeletedFalse(userId, targetDeviceId)
					.forEach(tokenDoc -> {
						tokenDoc.setDeleted(true);
						tokenDoc.setDeletedAt(now);
						processTokenRepository.save(tokenDoc);
					});

			userRepository.findByIdAndDeletedFalse(userId).ifPresent(user -> {
				if (user.getActiveDevices() != null) {
					user.getActiveDevices().removeIf(d -> targetDeviceId.equals(d.getDeviceId()));
				}
				user.setUpdatedAt(now);
				userRepository.save(user);
			});
		}

		return Map.of(
				"success", true,
				"message", "Sesión cerrada correctamente"
		);
	}

	public AuthResponse refreshToken(RefreshTokenRequest request) {
		String rawRefresh = request.refreshToken().trim();
		var claims = jwtHelper.validateToken(rawRefresh);
		ProcessTokenDocument tokenDoc = processTokenRepository.findByTokenAndTypeAndDeletedFalse(rawRefresh, "refreshToken")
				.orElseThrow(() -> CustomError.unauthorized("Refresh token inválido o revocado", "INVALID_REFRESH_TOKEN"));

		if (tokenDoc.getExpires().isBefore(Instant.now())) {
			throw CustomError.unauthorized("Refresh token expirado", "EXPIRED_REFRESH_TOKEN");
		}

		UserDocument user = userRepository.findByIdAndDeletedFalse(claims.getSubject())
				.orElseThrow(() -> CustomError.notFound("Usuario no encontrado", "USER_NOT_FOUND"));

		tokenDoc.setDeleted(true);
		tokenDoc.setDeletedAt(Instant.now());
		processTokenRepository.save(tokenDoc);

		String effectiveDeviceId = (request.deviceId() != null && !request.deviceId().isBlank())
				? request.deviceId().trim()
				: tokenDoc.getDeviceId();

		return issueTokensAndBuildResponse(user, effectiveDeviceId, "Token renovado exitosamente");
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
				.id(uuidGeneratorHelper.generate())
				.token(accessToken)
				.type("accessToken")
				.userId(user.getId())
				.deviceId(effectiveDeviceId)
				.expires(Instant.now().plusSeconds(24 * 60 * 60))
				.deleted(false)
				.build());

		processTokenRepository.save(ProcessTokenDocument.builder()
				.id(uuidGeneratorHelper.generate())
				.token(refreshToken)
				.type("refreshToken")
				.userId(user.getId())
				.deviceId(effectiveDeviceId)
				.expires(Instant.now().plusSeconds(180L * 24 * 60 * 60))
				.deleted(false)
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
