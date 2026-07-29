package com.careerlink.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.careerlink.backend.domain.SchoolType;
import com.careerlink.backend.domain.StudentSession;
import com.careerlink.backend.exception.ResourceNotFoundException;
import com.careerlink.backend.repository.StudentSessionRepository;

@Service
@Transactional(readOnly = true)
public class StudentSessionService {

	private final StudentSessionRepository studentSessionRepository;

	public StudentSessionService(StudentSessionRepository studentSessionRepository) {
		this.studentSessionRepository = studentSessionRepository;
	}

	@Transactional
	public StudentSession enter(String studentName, String studentPhone, SchoolType schoolType, Integer grade) {
		StudentSession studentSession = new StudentSession(studentName, studentPhone, schoolType, grade);

		return studentSessionRepository.saveAndFlush(studentSession);
	}

	public StudentSession getStudentSession(Long id) {
		return studentSessionRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("StudentSession", id));
	}
}
