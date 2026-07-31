package com.careerlink.backend.dto;

import java.time.LocalDateTime;

import com.careerlink.backend.domain.CounselorSession;

public record CounselorSessionResponse(
	Long id,
	String counselorName,
	Long topicId,
	String topicName,
	Long typeId,
	String typeName,
	LocalDateTime enteredAt
) {
	public static CounselorSessionResponse from(CounselorSession counselorSession) {
		Long topicId = (counselorSession.getType() != null && counselorSession.getType().getTopic() != null)
			? counselorSession.getType().getTopic().getId() : null;
		String topicName = (counselorSession.getType() != null && counselorSession.getType().getTopic() != null)
			? counselorSession.getType().getTopic().getName() : "";
		return new CounselorSessionResponse(
			counselorSession.getId(),
			counselorSession.getCounselorName(),
			topicId,
			topicName,
			counselorSession.getType().getId(),
			counselorSession.getType().getName(),
			counselorSession.getEnteredAt()
		);
	}
}
