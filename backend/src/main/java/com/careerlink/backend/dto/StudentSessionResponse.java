package com.careerlink.backend.dto;

import java.time.LocalDateTime;

import com.careerlink.backend.domain.StudentSession;

public record StudentSessionResponse(
	Long id,
	String studentName,
	String studentPhone,
	String schoolType,
	Integer grade,
	LocalDateTime enteredAt
) {
	public static StudentSessionResponse from(StudentSession studentSession) {
		return new StudentSessionResponse(
			studentSession.getId(),
			studentSession.getStudentName(),
			studentSession.getStudentPhone(),
			studentSession.getSchoolType().name(),
			studentSession.getGrade(),
			studentSession.getEnteredAt()
		);
	}
}
