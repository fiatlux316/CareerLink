package com.careerlink.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.careerlink.backend.domain.Consultation;
import com.careerlink.backend.domain.ConsultationStatus;
import com.careerlink.backend.domain.ConsultationType;
import com.careerlink.backend.domain.StudentSession;
import com.careerlink.backend.exception.ResourceNotFoundException;
import com.careerlink.backend.repository.ConsultationRepository;
import com.careerlink.backend.repository.ConsultationTypeRepository;
import com.careerlink.backend.repository.StudentSessionRepository;

@Service
@Transactional(readOnly = true)
public class ConsultationService {

	private final ConsultationRepository consultationRepository;
	private final ConsultationTypeRepository consultationTypeRepository;
	private final StudentSessionRepository studentSessionRepository;

	public ConsultationService(
		ConsultationRepository consultationRepository,
		ConsultationTypeRepository consultationTypeRepository,
		StudentSessionRepository studentSessionRepository
	) {
		this.consultationRepository = consultationRepository;
		this.consultationTypeRepository = consultationTypeRepository;
		this.studentSessionRepository = studentSessionRepository;
	}

	@Transactional
	public Consultation createConsultation(Long studentSessionId, Long typeId) {
		StudentSession studentSession = getStudentSession(studentSessionId);
		ConsultationType consultationType = getConsultationType(typeId);
		Consultation consultation = new Consultation(studentSession, consultationType);

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
		return consultationRepository.findByStudentSession_StudentPhoneOrderByCreatedAtDesc(studentPhone)
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

	private StudentSession getStudentSession(Long studentSessionId) {
		return studentSessionRepository.findById(studentSessionId)
			.orElseThrow(() -> new ResourceNotFoundException("StudentSession", studentSessionId));
	}

	private Consultation initializeConsultation(Consultation consultation) {
		consultation.getType().getId();
		consultation.getType().getName();
		consultation.getStudentSession().getId();
		consultation.getStudentSession().getStudentName();
		consultation.getStudentSession().getStudentPhone();
		consultation.getStudentSession().getSchoolType();
		consultation.getStudentSession().getGrade();
		return consultation;
	}
}
