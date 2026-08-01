package com.careerlink.backend.dto;

import java.time.LocalDateTime;

import com.careerlink.backend.domain.Consultation;
import com.careerlink.backend.domain.ConsultationResult;
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
	LocalDateTime updatedAt,
	String resultContent,
	Integer reConsultationNeeded,
	Integer satisfactionScore
) {
	public static ConsultationResponse from(Consultation consultation) {
		return from(consultation, null);
	}

	public static ConsultationResponse from(Consultation consultation, ConsultationResult result) {
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
			consultation.getUpdatedAt(),
			result != null ? result.getResultContent() : null,
			result != null ? result.getReConsultationNeeded() : null,
			result != null ? result.getSatisfactionScore() : null
		);
	}
}
