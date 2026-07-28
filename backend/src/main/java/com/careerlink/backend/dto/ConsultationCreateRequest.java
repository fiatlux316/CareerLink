package com.careerlink.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ConsultationCreateRequest(
	@NotBlank(message = "studentName은 필수입니다.")
	String studentName,

	@NotBlank(message = "studentPhone은 필수입니다.")
	@Pattern(
		regexp = "^(010\\d{8}|010-\\d{4}-\\d{4})$",
		message = "studentPhone은 010XXXXXXXX 또는 010-XXXX-XXXX 형식이어야 합니다."
	)
	String studentPhone,

	@NotNull(message = "typeId는 필수입니다.")
	Long typeId
) {
}
