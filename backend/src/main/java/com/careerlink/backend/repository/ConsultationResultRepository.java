package com.careerlink.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.careerlink.backend.domain.ConsultationResult;

public interface ConsultationResultRepository extends JpaRepository<ConsultationResult, Long> {
	Optional<ConsultationResult> findByConsultationId(Long consultationId);
	List<ConsultationResult> findByConsultationIdIn(List<Long> consultationIds);
}
