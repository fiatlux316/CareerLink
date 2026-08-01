package com.careerlink.backend;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.careerlink.backend.controller.ConsultationController;
import com.careerlink.backend.domain.Consultation;
import com.careerlink.backend.domain.ConsultationResult;
import com.careerlink.backend.domain.ConsultationType;
import com.careerlink.backend.domain.SchoolType;
import com.careerlink.backend.domain.StudentSession;
import com.careerlink.backend.dto.AcceptRequest;
import com.careerlink.backend.dto.ConsultationCompleteRequest;
import com.careerlink.backend.dto.ConsultationResponse;
import com.careerlink.backend.repository.ConsultationTypeRepository;
import com.careerlink.backend.repository.StudentSessionRepository;
import com.careerlink.backend.service.ConsultationService;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ConsultationResultIntegrationTest {

	@Autowired
	private ConsultationController consultationController;

	@Autowired
	private ConsultationService consultationService;

	@Autowired
	private StudentSessionRepository studentSessionRepository;

	@Autowired
	private ConsultationTypeRepository consultationTypeRepository;

	@Test
	void completeConsultationSavesResultAndReturnsSatisfactionScore() {
		StudentSession session = studentSessionRepository.save(
			new StudentSession("테스트학생", "010-1234-5678", SchoolType.HIGH_SCHOOL, 2, 1)
		);
		ConsultationType type = consultationTypeRepository.save(
			new ConsultationType(null, "결과테스트유형", "설명")
		);

		Consultation consultation = consultationService.createConsultation(session.getId(), type.getId());
		consultationController.acceptConsultation(consultation.getId(), new AcceptRequest("상담사김철수"));
		consultationController.startProgressConsultation(consultation.getId());

		ConsultationCompleteRequest completeRequest = new ConsultationCompleteRequest(
			"학생의 진로 관련 유의미한 조언을 전달함.",
			1, // 재상담 필요
			5  // 만족도 5점
		);

		ConsultationResponse response = consultationController.completeConsultation(consultation.getId(), completeRequest);

		assertThat(response.status().name()).isEqualTo("COMPLETED");
		assertThat(response.resultContent()).isEqualTo("학생의 진로 관련 유의미한 조언을 전달함.");
		assertThat(response.reConsultationNeeded()).isEqualTo(1);
		assertThat(response.satisfactionScore()).isEqualTo(5);

		ConsultationResult savedResult = consultationService.getConsultationResult(consultation.getId());
		assertThat(savedResult).isNotNull();
		assertThat(savedResult.getResultContent()).isEqualTo("학생의 진로 관련 유의미한 조언을 전달함.");
		assertThat(savedResult.getReConsultationNeeded()).isEqualTo(1);
		assertThat(savedResult.getSatisfactionScore()).isEqualTo(5);
	}
}
