package com.careerlink.backend.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record ErrorResponse(
	int status,
	String message,
	OffsetDateTime timestamp,
	Map<String, String> fieldErrors
) {
	public static ErrorResponse of(int status, String message) {
		return new ErrorResponse(status, message, OffsetDateTime.now(), Map.of());
	}

	public static ErrorResponse of(int status, String message, Map<String, String> fieldErrors) {
		return new ErrorResponse(status, message, OffsetDateTime.now(), fieldErrors);
	}
}
