package com.careerlink.backend.service;

import java.util.List;

import org.springframework.data.domain.Sort;
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
	public StudentSession enter(String studentName, String studentPhone, SchoolType schoolType, Integer grade, Integer gender) {
		StudentSession studentSession = new StudentSession(
			studentName,
			studentPhone,
			schoolType,
			grade != null ? grade : 0,
			gender != null ? gender : 0
		);

		return studentSessionRepository.saveAndFlush(studentSession);
	}

	public StudentSession getStudentSession(Long id) {
		return studentSessionRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("StudentSession", id));
	}

	public List<StudentSession> getAllStudentSessions() {
		return studentSessionRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
	}

	@Transactional
	public StudentSession updateStudentSession(Long id, String studentName, String studentPhone, SchoolType schoolType, Integer grade, Integer gender) {
		StudentSession studentSession = getStudentSession(id);
		studentSession.updateInfo(studentName, studentPhone, schoolType, grade, gender);
		return studentSessionRepository.save(studentSession);
	}
}
