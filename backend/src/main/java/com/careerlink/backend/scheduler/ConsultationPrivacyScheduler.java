package com.careerlink.backend.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.careerlink.backend.service.PrivacyMaskingService;

@Component
public class ConsultationPrivacyScheduler {

	private final PrivacyMaskingService privacyMaskingService;

	public ConsultationPrivacyScheduler(PrivacyMaskingService privacyMaskingService) {
		this.privacyMaskingService = privacyMaskingService;
	}

	@Scheduled(cron = "${careerlink.privacy.masking-cron}")
	public void maskExpiredConsultations() {
		privacyMaskingService.maskExpiredConsultations();
	}
}
