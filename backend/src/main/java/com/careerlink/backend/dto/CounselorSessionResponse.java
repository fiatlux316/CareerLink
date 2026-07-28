package com.careerlink.backend.dto;

import java.time.LocalDateTime;

import com.careerlink.backend.domain.CounselorSession;

public record CounselorSessionResponse(
	Long id,
	String counselorName,
	Long typeId,
	String typeName,
	LocalDateTime enteredAt
) {
	public static CounselorSessionResponse from(CounselorSession counselorSession) {
		return new CounselorSessionResponse(
			counselorSession.getId(),
			counselorSession.getCounselorName(),
			counselorSession.getType().getId(),
			counselorSession.getType().getName(),
			counselorSession.getEnteredAt()
		);
	}
}
