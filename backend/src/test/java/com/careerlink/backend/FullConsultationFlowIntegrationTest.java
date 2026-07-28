package com.careerlink.backend;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class FullConsultationFlowIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ConsultationTypeRepository consultationTypeRepository;

	@Test
	void fullConsultationFlowMatchesStudentAndCounselorSequence() throws Exception {
		Long targetTypeId = consultationTypeRepository.findAll().get(0).getId();
		Long otherTypeId = consultationTypeRepository.findAll().get(1).getId();

		mockMvc.perform(get("/api/types"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(5)))
			.andExpect(jsonPath("$[0].name", is("상담유형1")));

		String consultationResponseBody = mockMvc.perform(post("/api/consultations")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "studentName": "통합테스트학생",
					  "studentPhone": "01012345678",
					  "typeId": %d
					}
					""".formatted(targetTypeId)))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/consultations/")))
			.andExpect(jsonPath("$.status", is("RECEIVED")))
			.andExpect(jsonPath("$.typeId", is(targetTypeId.intValue())))
			.andReturn()
			.getResponse()
			.getContentAsString();

		JsonNode createdConsultation = objectMapper.readTree(consultationResponseBody);
		long consultationId = createdConsultation.get("id").asLong();
		String consultationPath = "/api/consultations/" + consultationId;

		mockMvc.perform(get(consultationPath))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id", is((int) consultationId)))
			.andExpect(jsonPath("$.status", is("RECEIVED")));

		mockMvc.perform(post("/api/counselor/enter")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "counselorName": "상담사동일유형",
					  "counselorPhone": "01087654321",
					  "typeId": %d
					}
					""".formatted(targetTypeId)))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/counselor/enter/")))
			.andExpect(jsonPath("$.counselorName", is("상담사동일유형")))
			.andExpect(jsonPath("$.typeId", is(targetTypeId.intValue())));

		mockMvc.perform(get("/api/counselor/consultations")
				.param("typeId", String.valueOf(targetTypeId))
				.param("status", "RECEIVED"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].id", is((int) consultationId)))
			.andExpect(jsonPath("$[0].status", is("RECEIVED")));

		mockMvc.perform(post("/api/counselor/enter")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "counselorName": "상담사다른유형",
					  "counselorPhone": "01099998888",
					  "typeId": %d
					}
					""".formatted(otherTypeId)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.typeId", is(otherTypeId.intValue())));

		mockMvc.perform(get("/api/counselor/consultations")
				.param("typeId", String.valueOf(otherTypeId))
				.param("status", "RECEIVED"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(0)));

		mockMvc.perform(patch(consultationPath + "/accept")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "counselorName": "상담사동일유형"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is("IN_PROGRESS")))
			.andExpect(jsonPath("$.counselorName", is("상담사동일유형")));

		mockMvc.perform(get(consultationPath))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is("IN_PROGRESS")))
			.andExpect(jsonPath("$.counselorName", is("상담사동일유형")));

		mockMvc.perform(patch(consultationPath + "/complete"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is("COMPLETED")));

		mockMvc.perform(get(consultationPath))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is("COMPLETED")))
			.andExpect(jsonPath("$.counselorName", is("상담사동일유형")));
	}
}
