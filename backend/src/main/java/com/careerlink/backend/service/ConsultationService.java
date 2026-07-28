package com.careerlink.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.careerlink.backend.domain.Consultation;
import com.careerlink.backend.domain.ConsultationStatus;
import com.careerlink.backend.domain.ConsultationType;
import com.careerlink.backend.exception.ResourceNotFoundException;
import com.careerlink.backend.repository.ConsultationRepository;
import com.careerlink.backend.repository.ConsultationTypeRepository;

@Service
@Transactional(readOnly = true)
public class ConsultationService {

	private final ConsultationRepository consultationRepository;
	private final ConsultationTypeRepository consultationTypeRepository;

	public ConsultationService(
		ConsultationRepository consultationRepository,
		ConsultationTypeRepository consultationTypeRepository
	) {
		this.consultationRepository = consultationRepository;
		this.consultationTypeRepository = consultationTypeRepository;
	}

	@Transactional
	public Consultation createConsultation(String studentName, String studentPhone, Long typeId) {
		ConsultationType consultationType = getConsultationType(typeId);
		Consultation consultation = new Consultation(studentName, studentPhone, consultationType);

		return initializeConsultation(consultationRepository.saveAndFlush(consultation));
	}

	public Consultation getConsultation(Long id) {
		Consultation consultation = consultationRepository.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Consultation", id));

		return initializeConsultation(consultation);
	}

	public List<Consultation> findByTypeAndStatus(Long typeId, ConsultationStatus status) {
		ConsultationType consultationType = getConsultationType(typeId);

		return consultationRepository.findByTypeAndStatus(consultationType, status)
			.stream()
			.map(this::initializeConsultation)
			.toList();
	}

	public List<Consultation> findByStudentPhone(String studentPhone) {
		return consultationRepository.findByStudentPhoneOrderByCreatedAtDesc(studentPhone)
			.stream()
			.map(this::initializeConsultation)
			.toList();
	}

	public List<Consultation> getAllConsultations() {
		return consultationRepository.findAllByOrderByCreatedAtDesc()
			.stream()
			.map(this::initializeConsultation)
			.toList();
	}

	@Transactional
	public Consultation accept(Long consultationId, String counselorName) {
		Consultation consultation = getConsultation(consultationId);
		consultation.accept(counselorName);

		return initializeConsultation(consultationRepository.saveAndFlush(consultation));
	}

	@Transactional
	public Consultation startProgress(Long consultationId) {
		Consultation consultation = getConsultation(consultationId);
		consultation.startProgress();

		return initializeConsultation(consultationRepository.saveAndFlush(consultation));
	}

	@Transactional
	public Consultation complete(Long consultationId) {
		Consultation consultation = getConsultation(consultationId);
		consultation.complete();

		return initializeConsultation(consultationRepository.saveAndFlush(consultation));
	}

	@Transactional
	public Consultation cancel(Long consultationId) {
		Consultation consultation = getConsultation(consultationId);
		consultation.cancel();

		return initializeConsultation(consultationRepository.saveAndFlush(consultation));
	}

	private ConsultationType getConsultationType(Long typeId) {
		return consultationTypeRepository.findById(typeId)
			.orElseThrow(() -> new ResourceNotFoundException("ConsultationType", typeId));
	}

	private Consultation initializeConsultation(Consultation consultation) {
		consultation.getType().getId();
		consultation.getType().getName();
		return consultation;
	}
}
