package com.careerlink.backend.dto;

import java.time.LocalDateTime;

import com.careerlink.backend.domain.Consultation;
import com.careerlink.backend.domain.ConsultationStatus;

public record ConsultationResponse(
	Long id,
	Long studentSessionId,
	String studentName,
	String studentPhone,
	String schoolType,
	Integer grade,
	Long typeId,
	String typeName,
	ConsultationStatus status,
	String counselorName,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
	public static ConsultationResponse from(Consultation consultation) {
		return new ConsultationResponse(
			consultation.getId(),
			consultation.getStudentSession().getId(),
			consultation.getStudentName(),
			consultation.getStudentPhone(),
			consultation.getStudentSession().getSchoolType().name(),
			consultation.getStudentSession().getGrade(),
			consultation.getType().getId(),
			consultation.getType().getName(),
			consultation.getStatus(),
			consultation.getCounselorName(),
			consultation.getCreatedAt(),
			consultation.getUpdatedAt()
		);
	}
}
