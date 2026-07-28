package com.careerlink.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.careerlink.backend.domain.ConsultationType;
import com.careerlink.backend.domain.CounselorSession;
import com.careerlink.backend.exception.ResourceNotFoundException;
import com.careerlink.backend.repository.ConsultationTypeRepository;
import com.careerlink.backend.repository.CounselorSessionRepository;

@Service
@Transactional(readOnly = true)
public class CounselorService {

	private final CounselorSessionRepository counselorSessionRepository;
	private final ConsultationTypeRepository consultationTypeRepository;

	public CounselorService(
		CounselorSessionRepository counselorSessionRepository,
		ConsultationTypeRepository consultationTypeRepository
	) {
		this.counselorSessionRepository = counselorSessionRepository;
		this.consultationTypeRepository = consultationTypeRepository;
	}

	@Transactional
	public CounselorSession enter(String counselorName, String counselorPhone, Long typeId) {
		ConsultationType consultationType = consultationTypeRepository.findById(typeId)
			.orElseThrow(() -> new ResourceNotFoundException("ConsultationType", typeId));
		CounselorSession counselorSession = new CounselorSession(counselorName, counselorPhone, consultationType);

		return initializeCounselorSession(counselorSessionRepository.saveAndFlush(counselorSession));
	}

	private CounselorSession initializeCounselorSession(CounselorSession counselorSession) {
		counselorSession.getType().getId();
		counselorSession.getType().getName();
		return counselorSession;
	}
}
