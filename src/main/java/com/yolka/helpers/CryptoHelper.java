package com.yolka.helpers;

import com.yolka.config.EnvsConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class CryptoHelper {

	private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
	private final EnvsConfig envsConfig;
	private final SecureRandom secureRandom = new SecureRandom();

	public record EncryptedPayload(String iv, String encryptedData) {
		public String toCombinedString() {
			return iv + "." + encryptedData;
		}
	}

	private SecretKeySpec getSecretKey() throws Exception {
		String rawKey = envsConfig.getEncryptionKey();
		if (rawKey == null || rawKey.isBlank()) {
			throw CustomError.internalServer("ENCRYPTION_KEY environment variable is not configured", "ENCRYPTION_KEY_MISSING");
		}
		byte[] keyBytes = MessageDigest.getInstance("SHA-256")
				.digest(rawKey.getBytes(StandardCharsets.UTF_8));
		return new SecretKeySpec(keyBytes, "AES");
	}

	public EncryptedPayload encryptBuffer(byte[] plainBytes) {
		try {
			byte[] ivBytes = new byte[16];
			secureRandom.nextBytes(ivBytes);
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), new IvParameterSpec(ivBytes));
			byte[] encrypted = cipher.doFinal(plainBytes);
			return new EncryptedPayload(
					Base64.getEncoder().encodeToString(ivBytes),
					Base64.getEncoder().encodeToString(encrypted)
			);
		} catch (CustomError ce) {
			throw ce;
		} catch (Exception e) {
			throw CustomError.internalServer("Encryption failed: " + e.getMessage(), "ENCRYPTION_ERROR");
		}
	}

	public byte[] decryptBuffer(String combinedPayload) {
		try {
			String[] parts = combinedPayload.split("\\.", 2);
			if (parts.length != 2) {
				throw CustomError.badRequest("Invalid encrypted backup format", "INVALID_ENCRYPTED_FORMAT");
			}
			byte[] ivBytes = Base64.getDecoder().decode(parts[0]);
			byte[] cipherBytes = Base64.getDecoder().decode(parts[1]);
			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), new IvParameterSpec(ivBytes));
			return cipher.doFinal(cipherBytes);
		} catch (CustomError ce) {
			throw ce;
		} catch (Exception e) {
			throw CustomError.internalServer("Decryption failed: " + e.getMessage(), "DECRYPTION_ERROR");
		}
	}

	public EncryptedPayload encryptData(String plainText) {
		return encryptBuffer(plainText.getBytes(StandardCharsets.UTF_8));
	}

	public String decryptData(String combinedPayload) {
		return new String(decryptBuffer(combinedPayload), StandardCharsets.UTF_8);
	}
}
