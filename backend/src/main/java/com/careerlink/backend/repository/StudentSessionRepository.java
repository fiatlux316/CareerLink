package com.careerlink.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.careerlink.backend.domain.StudentSession;

public interface StudentSessionRepository extends JpaRepository<StudentSession, Long> {

	List<StudentSession> findByStudentPhoneOrderByEnteredAtDesc(String studentPhone);
}
