package com.careerlink.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;

import com.careerlink.backend.domain.Consultation;
import com.careerlink.backend.domain.ConsultationStatus;
import com.careerlink.backend.domain.ConsultationType;
import com.careerlink.backend.exception.InvalidStatusTransitionException;
import com.careerlink.backend.exception.ResourceNotFoundException;
import com.careerlink.backend.repository.ConsultationTypeRepository;
import com.careerlink.backend.service.ConsultationService;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ConsultationServiceIntegrationTest {

	@Autowired
	private ConsultationService consultationService;

	@Autowired
	private ConsultationTypeRepository consultationTypeRepository;

	@Test
	void receivedConsultationCanBeAccepted() {
		Long typeId = consultationTypeRepository.findAll().get(0).getId();
		Consultation consultation = consultationService.createConsultation("학생1", "01011112222", typeId);

		Consultation acceptedConsultation = consultationService.accept(consultation.getId(), "상담사A");

		assertThat(acceptedConsultation.getStatus()).isEqualTo(ConsultationStatus.IN_PROGRESS);
		assertThat(acceptedConsultation.getCounselorName()).isEqualTo("상담사A");
		assertThat(acceptedConsultation.getUpdatedAt()).isNotNull();
	}

	@Test
	void inProgressConsultationCanBeCompleted() {
		Long typeId = consultationTypeRepository.findAll().get(0).getId();
		Consultation consultation = consultationService.createConsultation("학생2", "01022223333", typeId);

		consultationService.accept(consultation.getId(), "상담사B");
		Consultation completedConsultation = consultationService.complete(consultation.getId());

		assertThat(completedConsultation.getStatus()).isEqualTo(ConsultationStatus.COMPLETED);
	}

	@Test
	void receivedConsultationCanBeCancelled() {
		Long typeId = consultationTypeRepository.findAll().get(0).getId();
		Consultation consultation = consultationService.createConsultation("학생취소1", "01012121212", typeId);

		Consultation cancelledConsultation = consultationService.cancel(consultation.getId());

		assertThat(cancelledConsultation.getStatus()).isEqualTo(ConsultationStatus.CANCELLED);
	}

	@Test
	void receivedConsultationCannotBeCompletedDirectly() {
		Long typeId = consultationTypeRepository.findAll().get(0).getId();
		Consultation consultation = consultationService.createConsultation("학생3", "01033334444", typeId);

		assertThatThrownBy(() -> consultationService.complete(consultation.getId()))
			.isInstanceOf(InvalidStatusTransitionException.class)
			.hasMessageContaining("current=RECEIVED")
			.hasMessageContaining("target=COMPLETED");
	}

	@Test
	void completedConsultationCannotBeAcceptedAgain() {
		Long typeId = consultationTypeRepository.findAll().get(0).getId();
		Consultation consultation = consultationService.createConsultation("학생4", "01044445555", typeId);

		consultationService.accept(consultation.getId(), "상담사C");
		consultationService.complete(consultation.getId());

		assertThatThrownBy(() -> consultationService.accept(consultation.getId(), "상담사D"))
			.isInstanceOf(InvalidStatusTransitionException.class)
			.hasMessageContaining("current=COMPLETED")
			.hasMessageContaining("target=IN_PROGRESS");
	}

	@Test
	void inProgressConsultationCannotBeCancelled() {
		Long typeId = consultationTypeRepository.findAll().get(0).getId();
		Consultation consultation = consultationService.createConsultation("학생취소2", "01034343434", typeId);

		consultationService.accept(consultation.getId(), "상담사취소");

		assertThatThrownBy(() -> consultationService.cancel(consultation.getId()))
			.isInstanceOf(InvalidStatusTransitionException.class)
			.hasMessageContaining("current=IN_PROGRESS")
			.hasMessageContaining("target=CANCELLED");
	}

	@Test
	void completedConsultationCannotBeCancelled() {
		Long typeId = consultationTypeRepository.findAll().get(0).getId();
		Consultation consultation = consultationService.createConsultation("학생취소3", "01056565656", typeId);

		consultationService.accept(consultation.getId(), "상담사완료");
		consultationService.complete(consultation.getId());

		assertThatThrownBy(() -> consultationService.cancel(consultation.getId()))
			.isInstanceOf(InvalidStatusTransitionException.class)
			.hasMessageContaining("current=COMPLETED")
			.hasMessageContaining("target=CANCELLED");
	}

	@Test
	void findByTypeAndStatusReturnsOnlyReceivedConsultationsForRequestedType() {
		List<ConsultationType> consultationTypes = consultationTypeRepository.findAll();
		Long firstTypeId = consultationTypes.get(0).getId();
		Long secondTypeId = consultationTypes.get(1).getId();

		Consultation firstReceived = consultationService.createConsultation("학생5", "01055556666", firstTypeId);
		Consultation acceptedConsultation = consultationService.createConsultation("학생6", "01066667777", firstTypeId);
		consultationService.createConsultation("학생7", "01077778888", secondTypeId);
		consultationService.accept(acceptedConsultation.getId(), "상담사E");

		List<Consultation> receivedConsultations = consultationService.findByTypeAndStatus(firstTypeId, ConsultationStatus.RECEIVED);

		assertThat(receivedConsultations)
			.hasSize(1)
			.extracting(Consultation::getId)
			.containsExactly(firstReceived.getId());
	}

	@Test
	void createConsultationFailsWhenTypeDoesNotExist() {
		assertThatThrownBy(() -> consultationService.createConsultation("학생8", "01088889999", 999999L))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessageContaining("ConsultationType");
	}
}
