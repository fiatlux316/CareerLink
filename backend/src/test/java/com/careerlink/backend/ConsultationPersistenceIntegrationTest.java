package com.careerlink.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.careerlink.backend.domain.Consultation;
import com.careerlink.backend.domain.ConsultationStatus;
import com.careerlink.backend.domain.ConsultationType;
import com.careerlink.backend.domain.SchoolType;
import com.careerlink.backend.domain.StudentSession;
import com.careerlink.backend.repository.ConsultationRepository;
import com.careerlink.backend.repository.ConsultationTypeRepository;
import com.careerlink.backend.repository.StudentSessionRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConsultationPersistenceIntegrationTest {

	@Autowired
	private ConsultationTypeRepository consultationTypeRepository;

	@Autowired
	private ConsultationRepository consultationRepository;

	@Autowired
	private StudentSessionRepository studentSessionRepository;

	@Test
	void consultationTypesAreSeededOnStartup() {
		List<ConsultationType> consultationTypes = consultationTypeRepository.findAll();

		assertThat(consultationTypes).hasSize(5);
		assertThat(consultationTypes)
			.extracting(ConsultationType::getName)
			.containsExactly("상담유형1", "상담유형2", "상담유형3", "상담유형4", "상담유형5");
	}

	@Test
	void consultationPersistsWithDefaultStatusAndTimestamps() {
		ConsultationType consultationType = consultationTypeRepository.findAll().get(0);
		StudentSession studentSession = studentSessionRepository.saveAndFlush(
			new StudentSession("홍길동", "01012345678", SchoolType.MIDDLE_SCHOOL, 1)
		);
		Consultation consultation = new Consultation(studentSession, consultationType);

		Consultation savedConsultation = consultationRepository.saveAndFlush(consultation);

		assertThat(savedConsultation.getId()).isNotNull();
		assertThat(savedConsultation.getStatus()).isEqualTo(ConsultationStatus.RECEIVED);
		assertThat(savedConsultation.getCreatedAt()).isNotNull();
		assertThat(savedConsultation.getUpdatedAt()).isNotNull();
		assertThat(consultationRepository.findByTypeAndStatus(consultationType, ConsultationStatus.RECEIVED))
			.hasSize(1);
	}
}
