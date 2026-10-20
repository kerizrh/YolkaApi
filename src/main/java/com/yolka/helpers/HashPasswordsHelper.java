package com.yolka.helpers;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class HashPasswordsHelper {

	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

	public String hash(String plainPassword) {
		return encoder.encode(plainPassword);
	}

	public boolean compare(String plainPassword, String hashedPassword) {
		if (plainPassword == null || hashedPassword == null || hashedPassword.isBlank()) {
			return false;
		}
		return encoder.matches(plainPassword, hashedPassword);
	}
}
