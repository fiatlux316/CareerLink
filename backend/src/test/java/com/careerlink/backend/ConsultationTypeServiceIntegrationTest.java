package com.careerlink.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.careerlink.backend.domain.ConsultationType;
import com.careerlink.backend.exception.TypeInUseException;
import com.careerlink.backend.repository.ConsultationTypeRepository;
import com.careerlink.backend.service.ConsultationService;
import com.careerlink.backend.service.ConsultationTypeService;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ConsultationTypeServiceIntegrationTest {

	@Autowired
	private ConsultationTypeService consultationTypeService;

	@Autowired
	private ConsultationService consultationService;

	@Autowired
	private ConsultationTypeRepository consultationTypeRepository;

	@Test
	void createTypeCreatesNewConsultationType() {
		ConsultationType consultationType = consultationTypeService.createType("신규유형", "신규유형 설명");

		assertThat(consultationType.getId()).isNotNull();
		assertThat(consultationType.getName()).isEqualTo("신규유형");
		assertThat(consultationType.getDescription()).isEqualTo("신규유형 설명");
	}

	@Test
	void deleteTypeDeletesUnusedConsultationType() {
		ConsultationType consultationType = consultationTypeService.createType("삭제가능유형", "참조 없음");
                Long consultationTypeId = consultationType.getId();

		consultationTypeService.deleteType(consultationTypeId);

		assertThat(consultationTypeRepository.findById(consultationTypeId)).isEmpty();
	}

	@Test
	void deleteTypeThrowsWhenConsultationTypeIsReferenced() {
		ConsultationType consultationType = consultationTypeService.createType("삭제불가유형", "참조 있음");
		consultationService.createConsultation("참조학생", "01078787878", consultationType.getId());

		assertThatThrownBy(() -> consultationTypeService.deleteType(consultationType.getId()))
			.isInstanceOf(TypeInUseException.class)
			.hasMessageContaining("id=" + consultationType.getId())
			.hasMessageContaining("consultationCount=1");
	}
}
