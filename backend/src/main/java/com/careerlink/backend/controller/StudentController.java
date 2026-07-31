package com.careerlink.backend.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.careerlink.backend.domain.SchoolType;
import com.careerlink.backend.dto.StudentEnterRequest;
import com.careerlink.backend.dto.StudentSessionResponse;
import com.careerlink.backend.service.StudentSessionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/students")
public class StudentController {

	private final StudentSessionService studentSessionService;

	public StudentController(StudentSessionService studentSessionService) {
		this.studentSessionService = studentSessionService;
	}

	@PostMapping("/enter")
	public ResponseEntity<StudentSessionResponse> enter(@Valid @RequestBody StudentEnterRequest request) {
		SchoolType schoolType = parseSchoolType(request.schoolType());
		StudentSessionResponse response = StudentSessionResponse.from(
			studentSessionService.enter(request.studentName(), request.studentPhone(), schoolType, request.grade(), request.gender())
		);

		return ResponseEntity
			.created(URI.create("/api/students/enter/" + response.id()))
			.body(response);
	}

	private SchoolType parseSchoolType(String schoolType) {
		try {
			return SchoolType.valueOf(schoolType);
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("schoolType은 MIDDLE_SCHOOL, HIGH_SCHOOL 또는 MIDDLE_HIGH_SCHOOL 이어야 합니다.");
		}
	}
}
