package com.careerlink.backend.dto;

import com.careerlink.backend.domain.SchoolType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record StudentSessionUpdateRequest(
	@NotBlank(message = "학생 이름을 입력해주세요.")
	String studentName,

	@NotBlank(message = "휴대폰 번호를 입력해주세요.")
	@Pattern(
		regexp = "^01[0-9]-?\\d{3,4}-?\\d{4}$",
		message = "휴대폰 번호 형식이 올바르지 않습니다."
	)
	String studentPhone,

	@NotNull(message = "학교 구분을 선택해주세요.")
	SchoolType schoolType,

	@NotNull(message = "학년을 선택해주세요.")
	@Min(value = 0, message = "학년은 0 이상이어야 합니다.")
	@Max(value = 3, message = "학년은 3 이하여야 합니다.")
	Integer grade,

	@NotNull(message = "성별을 선택해주세요.")
	@Min(value = 0, message = "성별은 0 이상이어야 합니다.")
	@Max(value = 2, message = "성별은 2 이하여야 합니다.")
	Integer gender
) {
}
