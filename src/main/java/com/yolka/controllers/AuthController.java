package com.yolka.controllers;

import com.yolka.dto.AuthDtos.*;
import com.yolka.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping({"/api/v1/auth", "/api/auth"})
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/register")
	public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok(authService.login(request));
	}

	@PostMapping("/google")
	public ResponseEntity<AuthResponse> googleSignIn(@RequestBody GoogleLoginRequest request) {
		return ResponseEntity.ok(authService.googleSignIn(request));
	}

	@PostMapping("/recover-password")
	public ResponseEntity<Map<String, Object>> recoverPassword(@Valid @RequestBody RecoverPasswordRequest request) {
		return ResponseEntity.ok(authService.recoverPassword(request));
	}

	@PostMapping("/reset-password")
	public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		return ResponseEntity.ok(authService.resetPassword(request));
	}

	@GetMapping(value = "/confirm-account", produces = "text/html;charset=UTF-8")
	public ResponseEntity<String> confirmAccountByLink(@RequestParam("token") String token) {
		return ResponseEntity.ok(authService.confirmAccountByLink(token));
	}

	@PostMapping("/confirm-account")
	public ResponseEntity<Map<String, Object>> confirmAccountByCode(@Valid @RequestBody ConfirmAccountRequest request) {
		return ResponseEntity.ok(authService.confirmAccountByCode(request));
	}

	@PostMapping("/request-confirmation")
	public ResponseEntity<Map<String, Object>> requestAccountConfirmation(@Valid @RequestBody RequestConfirmationRequest request) {
		return ResponseEntity.ok(authService.requestAccountConfirmation(request));
	}

	@PostMapping("/logout")
	public ResponseEntity<Map<String, Object>> logout(
			@RequestHeader(value = "x-token", required = false) String xToken,
			@RequestHeader(value = "Authorization", required = false) String authHeader,
			@RequestBody(required = false) LogoutRequest request
	) {
		String token = xToken;
		if ((token == null || token.isBlank()) && authHeader != null && authHeader.startsWith("Bearer ")) {
			token = authHeader.substring(7).trim();
		}
		return ResponseEntity.ok(authService.logout(token, request));
	}

	@PostMapping("/refresh-token")
	public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
		return ResponseEntity.ok(authService.refreshToken(request));
	}
}
