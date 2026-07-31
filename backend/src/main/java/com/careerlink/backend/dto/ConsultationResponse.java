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
	Integer gender,
	Long topicId,
	String topicName,
	Long typeId,
	String typeName,
	ConsultationStatus status,
	String counselorName,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
	public static ConsultationResponse from(Consultation consultation) {
		Long topicId = consultation.getType().getTopic() != null ? consultation.getType().getTopic().getId() : null;
		String topicName = consultation.getType().getTopic() != null ? consultation.getType().getTopic().getName() : "";
		return new ConsultationResponse(
			consultation.getId(),
			consultation.getStudentSession().getId(),
			consultation.getStudentName(),
			consultation.getStudentPhone(),
			consultation.getStudentSession().getSchoolType().name(),
			consultation.getStudentSession().getGrade(),
			consultation.getStudentSession().getGender(),
			topicId,
			topicName,
			consultation.getType().getId(),
			consultation.getType().getName(),
			consultation.getStatus(),
			consultation.getCounselorName(),
			consultation.getCreatedAt(),
			consultation.getUpdatedAt()
		);
	}
}
