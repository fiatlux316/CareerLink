package com.careerlink.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.careerlink.backend.domain.Consultation;
import com.careerlink.backend.domain.ConsultationType;
import com.careerlink.backend.domain.SchoolType;
import com.careerlink.backend.domain.StudentSession;
import com.careerlink.backend.repository.ConsultationRepository;
import com.careerlink.backend.repository.ConsultationTypeRepository;
import com.careerlink.backend.repository.StudentSessionRepository;
import com.careerlink.backend.service.ConsultationService;
import com.careerlink.backend.service.PrivacyMaskingService;

import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PrivacyMaskingServiceIntegrationTest {

	@Autowired
	private PrivacyMaskingService privacyMaskingService;

	@Autowired
	private ConsultationService consultationService;

	@Autowired
	private ConsultationRepository consultationRepository;

	@Autowired
	private ConsultationTypeRepository consultationTypeRepository;

	@Autowired
	private StudentSessionRepository studentSessionRepository;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private Clock clock;

	@Test
	void maskExpiredConsultationsMasksOnlyEligibleCompletedRecords() {
		ConsultationType consultationType = consultationTypeRepository.findAll().get(0);
		LocalDateTime now = LocalDateTime.ofInstant(Instant.now(clock), ZoneId.systemDefault());

		Consultation oldCompleted = createCompletedConsultation(consultationType, "학생A", "01011112222", "상담사A");
		Consultation recentCompleted = createCompletedConsultation(consultationType, "학생B", "01022223333", "상담사B");
		Long receivedStudentSessionId = createStudentSession("학생C", "01033334444");
		Consultation receivedConsultation = consultationService.createConsultation(receivedStudentSessionId, consultationType.getId());
		Consultation alreadyMasked = createCompletedConsultation(consultationType, "학생D", "01044445555", "상담사C");

		updateConsultationTimestamps(oldCompleted.getId(), now.minusDays(31), null);
		updateConsultationTimestamps(recentCompleted.getId(), now.minusDays(10), null);
		updateConsultationTimestamps(receivedConsultation.getId(), now.minusDays(45), null);
		updateConsultationTimestamps(alreadyMasked.getId(), now.minusDays(31), now.minusDays(1));
		updateStudentSession(alreadyMasked.getStudentSession().getId(), "***", "***-****-****");

		int maskedCount = privacyMaskingService.maskExpiredConsultations();

		entityManager.flush();
		entityManager.clear();

		Consultation maskedConsultation = consultationRepository.findById(oldCompleted.getId()).orElseThrow();
		Consultation unmaskedRecentConsultation = consultationRepository.findById(recentCompleted.getId()).orElseThrow();
		Consultation unmaskedReceivedConsultation = consultationRepository.findById(receivedConsultation.getId()).orElseThrow();
		Consultation preservedMaskedConsultation = consultationRepository.findById(alreadyMasked.getId()).orElseThrow();

		assertThat(maskedCount).isEqualTo(1);
		assertThat(maskedConsultation.getStudentName()).isEqualTo("***");
		assertThat(maskedConsultation.getStudentPhone()).isEqualTo("***-****-****");
		assertThat(maskedConsultation.getMaskedAt()).isNotNull();

		assertThat(unmaskedRecentConsultation.getStudentName()).isEqualTo("학생B");
		assertThat(unmaskedRecentConsultation.getStudentPhone()).isEqualTo("01022223333");
		assertThat(unmaskedRecentConsultation.getMaskedAt()).isNull();

		assertThat(unmaskedReceivedConsultation.getStudentName()).isEqualTo("학생C");
		assertThat(unmaskedReceivedConsultation.getStudentPhone()).isEqualTo("01033334444");
		assertThat(unmaskedReceivedConsultation.getMaskedAt()).isNull();

		assertThat(preservedMaskedConsultation.getStudentName()).isEqualTo("***");
		assertThat(preservedMaskedConsultation.getStudentPhone()).isEqualTo("***-****-****");
		assertThat(preservedMaskedConsultation.getMaskedAt()).isEqualTo(now.minusDays(1));
	}

	private Long createStudentSession(String studentName, String studentPhone) {
		StudentSession studentSession = studentSessionRepository.saveAndFlush(
			new StudentSession(studentName, studentPhone, SchoolType.MIDDLE_SCHOOL, 1)
		);
		return studentSession.getId();
	}

	private Consultation createCompletedConsultation(
		ConsultationType consultationType,
		String studentName,
		String studentPhone,
		String counselorName
	) {
		Long studentSessionId = createStudentSession(studentName, studentPhone);
		Consultation consultation = consultationService.createConsultation(studentSessionId, consultationType.getId());
		consultationService.accept(consultation.getId(), counselorName);
		consultationService.startProgress(consultation.getId());
		return consultationService.complete(consultation.getId());
	}

	private void updateConsultationTimestamps(Long id, LocalDateTime updatedAt, LocalDateTime maskedAt) {
		entityManager.createQuery(
			"""
			update Consultation c
			set c.updatedAt = :updatedAt,
			    c.maskedAt = :maskedAt
			where c.id = :id
			"""
		)
			.setParameter("updatedAt", updatedAt)
			.setParameter("maskedAt", maskedAt)
			.setParameter("id", id)
			.executeUpdate();

		entityManager.flush();
		entityManager.clear();
	}

	private void updateStudentSession(Long studentSessionId, String studentName, String studentPhone) {
		entityManager.createQuery(
			"""
			update StudentSession s
			set s.studentName = :studentName,
			    s.studentPhone = :studentPhone
			where s.id = :id
			"""
		)
			.setParameter("studentName", studentName)
			.setParameter("studentPhone", studentPhone)
			.setParameter("id", studentSessionId)
			.executeUpdate();

		entityManager.flush();
		entityManager.clear();
	}
}
