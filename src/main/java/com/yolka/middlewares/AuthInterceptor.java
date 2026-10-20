package com.yolka.middlewares;

import com.yolka.helpers.CustomError;
import com.yolka.helpers.JwtHelper;
import com.yolka.models.ProcessTokenDocument;
import com.yolka.repositories.ProcessTokenRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

	private final JwtHelper jwtHelper;
	private final ProcessTokenRepository processTokenRepository;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			return true;
		}

		String token = request.getHeader("x-token");
		if (token == null || token.isBlank()) {
			String authHeader = request.getHeader("Authorization");
			if (authHeader != null && authHeader.startsWith("Bearer ")) {
				token = authHeader.substring(7).trim();
			}
		}

		if (token != null && !token.isBlank()) {
			Claims claims = jwtHelper.validateToken(token);
			Optional<ProcessTokenDocument> tokenDoc = processTokenRepository.findByTokenAndTypeAndDeletedFalse(token, "accessToken");
			if (tokenDoc.isEmpty()) {
				throw CustomError.unauthorized("La sesión ha sido cerrada o el token fue revocado", "SESSION_REVOKED");
			}
			request.setAttribute("authenticatedUserId", claims.getSubject());
			request.setAttribute("authenticatedEmail", claims.get("email", String.class));
			request.setAttribute("authenticatedDeviceId", claims.get("deviceId", String.class));
		}

		return true;
	}
}
