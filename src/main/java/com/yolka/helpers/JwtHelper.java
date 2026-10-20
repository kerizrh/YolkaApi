package com.yolka.helpers;

import com.yolka.config.EnvsConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtHelper {

	private final EnvsConfig envsConfig;

	private SecretKey getSigningKey() {
		try {
			String rawSecret = envsConfig.getJwtSecret();
			if (rawSecret == null || rawSecret.isBlank()) {
				throw CustomError.internalServer("JWT_SECRET environment variable is not configured", "JWT_SECRET_MISSING");
			}
			byte[] keyBytes = MessageDigest.getInstance("SHA-256")
					.digest(rawSecret.getBytes(StandardCharsets.UTF_8));
			return Keys.hmacShaKeyFor(keyBytes);
		} catch (CustomError ce) {
			throw ce;
		} catch (Exception e) {
			throw CustomError.internalServer("Failed to initialize JWT signing key", "JWT_KEY_ERROR");
		}
	}

	public String generateAccessToken(String userId, String email, String deviceId) {
		long twentyFourHoursMs = 24L * 60 * 60 * 1000;
		return generateToken(userId, email, deviceId, "accessToken", twentyFourHoursMs);
	}

	public String generateRefreshToken(String userId, String email, String deviceId) {
		long oneEightyDaysMs = 180L * 24 * 60 * 60 * 1000;
		return generateToken(userId, email, deviceId, "refreshToken", oneEightyDaysMs);
	}

	public String generateOneUseToken(String userId, String email, String type, long durationMs) {
		return generateToken(userId, email, "system", type, durationMs);
	}

	public String generateToken(String userId, String email, String deviceId, String type, long durationMs) {
		Instant now = Instant.now();
		return Jwts.builder()
				.subject(userId)
				.claims(Map.of(
						"id", userId,
						"email", email != null ? email : "",
						"deviceId", deviceId != null ? deviceId : "",
						"type", type
				))
				.issuedAt(Date.from(now))
				.expiration(Date.from(now.plusMillis(durationMs)))
				.signWith(getSigningKey())
				.compact();
	}

	public Claims validateToken(String token) {
		try {
			return Jwts.parser()
					.verifyWith(getSigningKey())
					.build()
					.parseSignedClaims(token)
					.getPayload();
		} catch (Exception e) {
			throw CustomError.unauthorized("Invalid or expired token", "INVALID_TOKEN");
		}
	}
}
