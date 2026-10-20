package com.yolka.helpers;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CustomError extends RuntimeException {

	private final int statusCode;
	private final String code;

	public CustomError(int statusCode, String message, String code) {
		super(message);
		this.statusCode = statusCode;
		this.code = code;
	}

	public static CustomError badRequest(String message, String code) {
		return new CustomError(HttpStatus.BAD_REQUEST.value(), message, code);
	}

	public static CustomError unauthorized(String message, String code) {
		return new CustomError(HttpStatus.UNAUTHORIZED.value(), message, code);
	}

	public static CustomError forbidden(String message, String code) {
		return new CustomError(HttpStatus.FORBIDDEN.value(), message, code);
	}

	public static CustomError notFound(String message, String code) {
		return new CustomError(HttpStatus.NOT_FOUND.value(), message, code);
	}

	public static CustomError internalServer(String message, String code) {
		return new CustomError(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, code);
	}
}
