package com.careerlink.backend.dto;

import com.careerlink.backend.domain.ConsultationType;

public record ConsultationTypeResponse(
	Long id,
	Long topicId,
	String topicName,
	String name,
	String description
) {
	public static ConsultationTypeResponse from(ConsultationType consultationType) {
		Long topicId = consultationType.getTopic() != null ? consultationType.getTopic().getId() : null;
		String topicName = consultationType.getTopic() != null ? consultationType.getTopic().getName() : "";
		return new ConsultationTypeResponse(
			consultationType.getId(),
			topicId,
			topicName,
			consultationType.getName(),
			consultationType.getDescription()
		);
	}
}
