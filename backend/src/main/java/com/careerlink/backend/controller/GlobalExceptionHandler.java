package com.careerlink.backend.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;

import com.careerlink.backend.dto.ErrorResponse;
import com.careerlink.backend.exception.InvalidStatusTransitionException;
import com.careerlink.backend.exception.ResourceNotFoundException;
import com.careerlink.backend.exception.TypeInUseException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(ErrorResponse.of(HttpStatus.NOT_FOUND.value(), exception.getMessage()));
	}

	@ExceptionHandler(InvalidStatusTransitionException.class)
	public ResponseEntity<ErrorResponse> handleInvalidStatusTransition(InvalidStatusTransitionException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(ErrorResponse.of(HttpStatus.CONFLICT.value(), exception.getMessage()));
	}

	@ExceptionHandler(TypeInUseException.class)
	public ResponseEntity<ErrorResponse> handleTypeInUse(TypeInUseException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(ErrorResponse.of(HttpStatus.CONFLICT.value(), exception.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
		return ResponseEntity.badRequest()
			.body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), exception.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();

		for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
			fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
		}

		return ResponseEntity.badRequest()
			.body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "입력값 검증에 실패했습니다.", fieldErrors));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
		MissingServletRequestParameterException exception
	) {
		return ResponseEntity.badRequest()
			.body(ErrorResponse.of(
				HttpStatus.BAD_REQUEST.value(),
				exception.getParameterName() + " 파라미터는 필수입니다."
			));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();

		for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
			fieldErrors.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage());
		}

		return ResponseEntity.badRequest()
			.body(ErrorResponse.of(HttpStatus.BAD_REQUEST.value(), "요청 파라미터 검증에 실패했습니다.", fieldErrors));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception exception) {
		log.error("Unhandled exception", exception);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ErrorResponse.of(HttpStatus.INTERNAL_SERVER_ERROR.value(), "서버 내부 오류가 발생했습니다."));
	}
}
