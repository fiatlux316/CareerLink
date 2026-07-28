package com.careerlink.backend.dto;

import com.careerlink.backend.domain.ConsultationType;

public record ConsultationTypeResponse(
	Long id,
	String name,
	String description
) {
	public static ConsultationTypeResponse from(ConsultationType consultationType) {
		return new ConsultationTypeResponse(
			consultationType.getId(),
			consultationType.getName(),
			consultationType.getDescription()
		);
	}
}
