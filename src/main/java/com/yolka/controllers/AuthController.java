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
@RequestMapping("/api/v1/auth")
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
}
