package com.careerlink.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConsultationTypeCreateRequest(
	@NotNull(message = "topicId는 필수입니다.")
	Long topicId,

	@NotBlank(message = "name은 필수입니다.")
	String name,

	@NotBlank(message = "description은 필수입니다.")
	String description
) {
}
