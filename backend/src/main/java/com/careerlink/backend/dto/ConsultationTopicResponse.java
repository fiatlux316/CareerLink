package com.careerlink.backend.dto;

import com.careerlink.backend.domain.ConsultationTopic;

public record ConsultationTopicResponse(
	Long id,
	String name,
	String description
) {
	public static ConsultationTopicResponse from(ConsultationTopic topic) {
		return new ConsultationTopicResponse(
			topic.getId(),
			topic.getName(),
			topic.getDescription()
		);
	}
}
