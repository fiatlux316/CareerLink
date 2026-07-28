package com.careerlink.backend;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.careerlink.backend.repository.ConsultationTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.open-in-view=true")
@Transactional
class PrdV3ScenarioIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ConsultationTypeRepository consultationTypeRepository;

	@Test
	void studentCanCancelReceivedConsultationAndCancelledStatusPersists() throws Exception {
		Long typeId = consultationTypeRepository.findAll().get(0).getId();
		String location = createConsultation("취소시나리오학생", "01010001000", typeId);

		mockMvc.perform(patch(location + "/cancel"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is("CANCELLED")));

		mockMvc.perform(get(location))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is("CANCELLED")));
	}

	@Test
	void inProgressConsultationCannotBeCancelledAfterCounselorEntryAndAccept() throws Exception {
		Long typeId = consultationTypeRepository.findAll().get(0).getId();
		String location = createConsultation("진행중취소불가학생", "01020002000", typeId);

		enterCounselor("상담사취소불가", "01090009000", typeId);

		mockMvc.perform(patch(location + "/accept")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "counselorName": "상담사취소불가"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is("ACCEPTED")));

		mockMvc.perform(patch(location + "/cancel"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status", is(409)));
	}

	@Test
	void adminTypeDeleteRemainsBlockedWhenAnyConsultationReferenceExists() throws Exception {
		Long referencedTypeId = extractId(createType("PRD v3 참조유형", "삭제 전 참조 확인"));
		String referencedConsultationLocation = createConsultation("유형참조학생", "01030003000", referencedTypeId);

		mockMvc.perform(delete("/api/admin/types/{id}", referencedTypeId))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status", is(409)));

		enterCounselor("상담사참조", "01091009100", referencedTypeId);

		mockMvc.perform(patch(referencedConsultationLocation + "/accept")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "counselorName": "상담사참조"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is("ACCEPTED")));

		mockMvc.perform(patch(referencedConsultationLocation + "/start-progress"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is("IN_PROGRESS")));

		mockMvc.perform(patch(referencedConsultationLocation + "/complete"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is("COMPLETED")));

		mockMvc.perform(delete("/api/admin/types/{id}", referencedTypeId))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status", is(409)));

		Long unusedTypeId = extractId(createType("PRD v3 미사용유형", "삭제 성공 확인"));

		mockMvc.perform(delete("/api/admin/types/{id}", unusedTypeId))
			.andExpect(status().isNoContent());
	}

	@Test
	void consultationListByStudentPhoneKeepsCancelledItemsInLatestOrder() throws Exception {
		Long firstTypeId = consultationTypeRepository.findAll().get(0).getId();
		Long secondTypeId = consultationTypeRepository.findAll().get(1).getId();
		Long thirdTypeId = consultationTypeRepository.findAll().get(2).getId();
		String studentPhone = "01040004000";

		String firstLocation = createConsultation("다건학생", studentPhone, firstTypeId);
		String secondLocation = createConsultation("다건학생", studentPhone, secondTypeId);
		String thirdLocation = createConsultation("다건학생", studentPhone, thirdTypeId);

		mockMvc.perform(patch(secondLocation + "/cancel"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is("CANCELLED")));

		mockMvc.perform(get("/api/consultations")
				.param("studentPhone", studentPhone))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(3)))
			.andExpect(jsonPath("$[0].id", is(extractId(thirdLocation).intValue())))
			.andExpect(jsonPath("$[0].status", is("RECEIVED")))
			.andExpect(jsonPath("$[1].id", is(extractId(secondLocation).intValue())))
			.andExpect(jsonPath("$[1].status", is("CANCELLED")))
			.andExpect(jsonPath("$[2].id", is(extractId(firstLocation).intValue())))
			.andExpect(jsonPath("$[2].status", is("RECEIVED")));
	}

	private String createConsultation(String studentName, String studentPhone, Long typeId) throws Exception {
		return mockMvc.perform(post("/api/consultations")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "studentName": "%s",
					  "studentPhone": "%s",
					  "typeId": %d
					}
					""".formatted(studentName, studentPhone, typeId)))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/consultations/")))
			.andReturn()
			.getResponse()
			.getHeader("Location");
	}

	private String createType(String name, String description) throws Exception {
		return mockMvc.perform(post("/api/admin/types")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "name": "%s",
					  "description": "%s"
					}
					""".formatted(name, description)))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/admin/types/")))
			.andReturn()
			.getResponse()
			.getHeader("Location");
	}

	private void enterCounselor(String counselorName, String counselorPhone, Long typeId) throws Exception {
		mockMvc.perform(post("/api/counselor/enter")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "counselorName": "%s",
					  "counselorPhone": "%s",
					  "typeId": %d
					}
					""".formatted(counselorName, counselorPhone, typeId)))
			.andExpect(status().isCreated());
	}

	private Long extractId(String location) throws Exception {
		return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
	}
}
