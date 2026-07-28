package com.careerlink.backend.exception;

import com.careerlink.backend.domain.ConsultationStatus;

public class InvalidStatusTransitionException extends RuntimeException {

	public InvalidStatusTransitionException(ConsultationStatus currentStatus, ConsultationStatus targetStatus) {
		super("Invalid consultation status transition. current=" + currentStatus + ", target=" + targetStatus);
	}
}
