package com.careerlink.backend.dto;

import jakarta.validation.constraints.NotNull;

public record ConsultationCreateRequest(
	@NotNull(message = "studentSessionId는 필수입니다.")
	Long studentSessionId,

	@NotNull(message = "typeId는 필수입니다.")
	Long typeId
) {
}
