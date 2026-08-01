package com.careerlink.backend.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.careerlink.backend.domain.Consultation;
import com.careerlink.backend.domain.ConsultationResult;
import com.careerlink.backend.domain.ConsultationStatus;
import com.careerlink.backend.domain.ConsultationType;
import com.careerlink.backend.domain.StudentSession;
import com.careerlink.backend.dto.ConsultationCompleteRequest;
import com.careerlink.backend.exception.ResourceNotFoundException;
import com.careerlink.backend.repository.ConsultationRepository;
import com.careerlink.backend.repository.ConsultationResultRepository;
import com.careerlink.backend.repository.ConsultationTypeRepository;
import com.careerlink.backend.repository.StudentSessionRepository;

@Service
@Transactional(readOnly = true)
public class ConsultationService {

	private final ConsultationRepository consultationRepository;
	private final ConsultationTypeRepository consultationTypeRepository;
	private final StudentSessionRepository studentSessionRepository;
	private final ConsultationResultRepository consultationResultRepository;
	private final NotificationService notificationService;

	public ConsultationService(
		ConsultationRepository consultationRepository,
		ConsultationTypeRepository consultationTypeRepository,
		StudentSessionRepository studentSessionRepository,
		ConsultationResultRepository consultationResultRepository,
		NotificationService notificationService
	) {
		this.consultationRepository = consultationRepository;
		this.consultationTypeRepository = consultationTypeRepository;
		this.studentSessionRepository = studentSessionRepository;
		this.consultationResultRepository = consultationResultRepository;
		this.notificationService = notificationService;
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

	public ConsultationResult getConsultationResult(Long consultationId) {
		return consultationResultRepository.findByConsultationId(consultationId).orElse(null);
	}

	public List<Consultation> findByTypeAndStatus(Long typeId, ConsultationStatus status) {
		return findByTypeAndStatus(typeId, status, null);
	}

	public List<Consultation> findByTypeAndStatus(Long typeId, ConsultationStatus status, String counselorName) {
		ConsultationType consultationType = getConsultationType(typeId);

		if (status == ConsultationStatus.RECEIVED || counselorName == null || counselorName.isBlank()) {
			return consultationRepository.findByTypeAndStatus(consultationType, status)
				.stream()
				.map(this::initializeConsultation)
				.toList();
		}

		return consultationRepository.findByTypeAndStatusAndCounselorName(consultationType, status, counselorName)
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
		Consultation saved = consultationRepository.saveAndFlush(consultation);

		notificationService.sendAcceptNotification(
			saved.getStudentName(),
			saved.getStudentPhone(),
			counselorName,
			saved.getType().getName()
		);

		return initializeConsultation(saved);
	}

	@Transactional
	public Consultation cancelAccept(Long consultationId) {
		Consultation consultation = getConsultation(consultationId);
		consultation.cancelAccept();

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
		return complete(consultationId, null);
	}

	@Transactional
	public Consultation complete(Long consultationId, ConsultationCompleteRequest request) {
		Consultation consultation = getConsultation(consultationId);
		consultation.complete();
		Consultation saved = consultationRepository.saveAndFlush(consultation);

		if (request != null && (request.resultContent() != null || request.reConsultationNeeded() != null || request.satisfactionScore() != null)) {
			ConsultationResult result = new ConsultationResult(
				saved,
				request.resultContent() != null ? request.resultContent().trim() : "",
				request.reConsultationNeeded() != null ? request.reConsultationNeeded() : 2,
				request.satisfactionScore() != null ? request.satisfactionScore() : 5
			);
			consultationResultRepository.saveAndFlush(result);
		}

		return initializeConsultation(saved);
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
		consultation.getStudentSession().getStudentName();
		consultation.getType().getName();
		if (consultation.getType().getTopic() != null) {
			consultation.getType().getTopic().getName();
		}
		return consultation;
	}
}
