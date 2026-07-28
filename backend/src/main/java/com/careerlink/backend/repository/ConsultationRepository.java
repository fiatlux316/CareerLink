package com.careerlink.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.careerlink.backend.domain.Consultation;
import com.careerlink.backend.domain.ConsultationStatus;
import com.careerlink.backend.domain.ConsultationType;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {

	List<Consultation> findByTypeAndStatus(ConsultationType type, ConsultationStatus status);

	List<Consultation> findByStatusAndUpdatedAtBeforeAndMaskedAtIsNull(
		ConsultationStatus status,
		LocalDateTime threshold
	);

	long countByType(ConsultationType type);

	List<Consultation> findByStudentPhoneOrderByCreatedAtDesc(String studentPhone);

	List<Consultation> findByStatusOrderByCreatedAtAsc(ConsultationStatus status);
}
