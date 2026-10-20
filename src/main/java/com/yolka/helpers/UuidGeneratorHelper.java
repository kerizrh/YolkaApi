package com.yolka.helpers;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UuidGeneratorHelper {

	public String generate() {
		return UUID.randomUUID().toString();
	}

	public static String newUuid() {
		return UUID.randomUUID().toString();
	}
}
