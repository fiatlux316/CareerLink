package com.careerlink.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record StudentEnterRequest(
	@NotBlank(message = "studentName은 필수입니다.")
	String studentName,

	@NotBlank(message = "studentPhone은 필수입니다.")
	@Pattern(
		regexp = "^(010\\d{8}|010-\\d{4}-\\d{4})$",
		message = "studentPhone은 010XXXXXXXX 또는 010-XXXX-XXXX 형식이어야 합니다."
	)
	String studentPhone,

	@NotBlank(message = "schoolType은 필수입니다.")
	String schoolType,

	@NotNull(message = "grade는 필수입니다.")
	@Min(value = 0, message = "grade는 0 이상이어야 합니다.")
	@Max(value = 3, message = "grade는 3 이하여야 합니다.")
	Integer grade,

	@Min(value = 0, message = "gender는 0 이상이어야 합니다.")
	@Max(value = 2, message = "gender는 2 이하여야 합니다.")
	Integer gender
) {
}
