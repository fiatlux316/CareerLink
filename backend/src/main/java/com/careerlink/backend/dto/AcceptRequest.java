package com.careerlink.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record AcceptRequest(
	@NotBlank(message = "counselorName은 필수입니다.")
	String counselorName
) {
}
