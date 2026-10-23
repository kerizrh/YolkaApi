package com.yolka.helpers;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.yolka.config.EnvsConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleVerifyHelper {

	private final EnvsConfig envsConfig;
	private final FirebaseAdminHelper firebaseAdminHelper;

	public record GoogleUserPayload(
			String sub,
			String email,
			String name,
			String givenName,
			String familyName,
			String picture
	) {}

	public GoogleUserPayload verifyGoogleSignIn(String idTokenString, String authorizationCode) {
		String tokenToVerify = idTokenString;

		// 1. Si enviaron authorizationCode y tenemos clientSecret configurado, intercambiar por id_token 
		if ((tokenToVerify == null || tokenToVerify.isBlank())
				&& authorizationCode != null && !authorizationCode.isBlank()
				&& envsConfig.getGoogleWebClientSecret() != null && !envsConfig.getGoogleWebClientSecret().isBlank()) {
			try {
				GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
						new NetHttpTransport(),
						GsonFactory.getDefaultInstance(),
						"https://oauth2.googleapis.com/token",
						envsConfig.getGoogleWebClientId(),
						envsConfig.getGoogleWebClientSecret(),
						authorizationCode,
						envsConfig.getGoogleRedirectUri() != null ? envsConfig.getGoogleRedirectUri() : ""
				).execute();
				tokenToVerify = tokenResponse.getIdToken();
			} catch (Exception e) {
				log.warn("Error exchanging Google authorizationCode: {}", e.getMessage());
				throw CustomError.badRequest("Invalid Google authorization code: " + e.getMessage(), "INVALID_GOOGLE_AUTH_CODE");
			}
		}

		if (tokenToVerify == null || tokenToVerify.isBlank()) {
			throw CustomError.badRequest("Google idToken or authorizationCode is required", "GOOGLE_TOKEN_REQUIRED");
		}

		// 2. Intentar verificar como Firebase Auth ID Token si Firebase Admin está inicializado
		if (firebaseAdminHelper.isInitialized()) {
			try {
				FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(tokenToVerify);
				String fullName = decodedToken.getName() != null ? decodedToken.getName() : "";
				String[] parts = fullName.split(" ", 2);
				return new GoogleUserPayload(
						decodedToken.getUid(),
						decodedToken.getEmail(),
						fullName,
						parts.length > 0 ? parts[0] : fullName,
						parts.length > 1 ? parts[1] : "",
						decodedToken.getPicture() != null ? decodedToken.getPicture() : ""
				);
			} catch (Exception ignored) {
				// No es un token de Firebase Auth, continuar con GoogleIdTokenVerifier estándar
			}
		}

		// 3. Verificar como Google OAuth2 ID Token
		try {
			GoogleIdTokenVerifier.Builder verifierBuilder = new GoogleIdTokenVerifier.Builder(
					new NetHttpTransport(),
					GsonFactory.getDefaultInstance()
			);
			List<String> audiences = envsConfig.getAllowedGoogleClientIds();
			if (!audiences.isEmpty()) {
				verifierBuilder.setAudience(audiences);
			}
			GoogleIdToken idToken = verifierBuilder.build().verify(tokenToVerify);
			if (idToken == null) {
				// Intentar parsear y verificar firma general de Google si el audience es de otro cliente del mismo proyecto
				idToken = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
						.build()
						.verify(tokenToVerify);
			}
			if (idToken == null) {
				throw CustomError.unauthorized("Invalid Google ID token", "INVALID_GOOGLE_TOKEN");
			}
			GoogleIdToken.Payload payload = idToken.getPayload();
			return new GoogleUserPayload(
					payload.getSubject(),
					payload.getEmail(),
					(String) payload.getOrDefault("name", ""),
					(String) payload.getOrDefault("given_name", ""),
					(String) payload.getOrDefault("family_name", ""),
					(String) payload.getOrDefault("picture", "")
			);
		} catch (CustomError ce) {
			throw ce;
		} catch (Exception e) {
			throw CustomError.unauthorized("Google token verification failed: " + e.getMessage(), "GOOGLE_VERIFY_ERROR");
		}
	}
}
