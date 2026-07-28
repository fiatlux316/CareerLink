package com.careerlink.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ConsultationTypeUpdateRequest(
	@NotBlank(message = "name은 필수입니다.")
	String name,

	@NotBlank(message = "description은 필수입니다.")
	String description
) {
}
