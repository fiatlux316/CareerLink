package com.careerlink.backend.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.careerlink.backend.config.PrivacyProperties;
import com.careerlink.backend.domain.Consultation;
import com.careerlink.backend.domain.ConsultationStatus;
import com.careerlink.backend.repository.ConsultationRepository;

@Service
@Transactional
public class PrivacyMaskingService {

	private final ConsultationRepository consultationRepository;
	private final PrivacyProperties privacyProperties;
	private final Clock clock;

	public PrivacyMaskingService(
		ConsultationRepository consultationRepository,
		PrivacyProperties privacyProperties,
		Clock clock
	) {
		this.consultationRepository = consultationRepository;
		this.privacyProperties = privacyProperties;
		this.clock = clock;
	}

	public int maskExpiredConsultations() {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDateTime threshold = now.minusDays(privacyProperties.getRetentionDays());
		List<Consultation> expiredConsultations = consultationRepository
			.findByStatusAndUpdatedAtBeforeAndMaskedAtIsNull(ConsultationStatus.COMPLETED, threshold);

		expiredConsultations.forEach(consultation -> consultation.maskSensitiveData(now));
		consultationRepository.saveAll(expiredConsultations);

		return expiredConsultations.size();
	}
}
