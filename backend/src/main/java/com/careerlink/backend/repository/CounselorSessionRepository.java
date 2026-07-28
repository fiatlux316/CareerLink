package com.careerlink.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.careerlink.backend.domain.ConsultationType;
import com.careerlink.backend.domain.CounselorSession;

public interface CounselorSessionRepository extends JpaRepository<CounselorSession, Long> {

	List<CounselorSession> findByType(ConsultationType type);
}
