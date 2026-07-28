package com.careerlink.backend.exception;

public class TypeInUseException extends RuntimeException {

	public TypeInUseException(Long typeId, long consultationCount) {
		super("ConsultationType is in use. id=" + typeId + ", consultationCount=" + consultationCount);
	}
}
