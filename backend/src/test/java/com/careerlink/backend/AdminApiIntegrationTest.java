package com.careerlink.backend;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.careerlink.backend.repository.ConsultationTypeRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.open-in-view=true")
@Transactional
class AdminApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ConsultationTypeRepository consultationTypeRepository;

	@Test
	void getAdminTypesReturnsFiveConsultationTypes() throws Exception {
		mockMvc.perform(get("/api/admin/types"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(5)))
				.andExpect(jsonPath("$[0].name", is("고상해")));
	}

	@Test
	void updateTypeReturnsUpdatedConsultationType() throws Exception {
		mockMvc.perform(put("/api/admin/types/{id}", 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "topicId": 1,
						  "name": "진로 상담",
						  "description": "진로 및 학업 관련 상담"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(1)))
				.andExpect(jsonPath("$.name", is("진로 상담")))
				.andExpect(jsonPath("$.description", is("진로 및 학업 관련 상담")));
	}

	@Test
	void createTypeReturnsCreatedConsultationType() throws Exception {
		mockMvc.perform(post("/api/admin/types")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "topicId": 1,
						  "name": "신규 유형",
						  "description": "신규 유형 설명"
						}
						"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/admin/types/")))
				.andExpect(jsonPath("$.id", org.hamcrest.Matchers.greaterThanOrEqualTo(6)))
				.andExpect(jsonPath("$.name", is("신규 유형")))
				.andExpect(jsonPath("$.description", is("신규 유형 설명")));
	}

	@Test
	void updateTypeAllowsIdsBeyondInitialSeedRangeWhenTypeExists() throws Exception {
		String location = mockMvc.perform(post("/api/admin/types")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "topicId": 1,
						  "name": "확장 유형",
						  "description": "추가 생성 후 수정"
						}
						"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getHeader("Location");

		Long createdId = extractId(location);

		mockMvc.perform(put("/api/admin/types/{id}", createdId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "topicId": 1,
						  "name": "확장 유형 수정",
						  "description": "범위 제약 제거 확인"
						}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", is(createdId.intValue())))
				.andExpect(jsonPath("$.name", is("확장 유형 수정")));
	}

	@Test
	void updateTypeReturnsBadRequestWhenIdIsNotPositive() throws Exception {
		mockMvc.perform(put("/api/admin/types/{id}", 0)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "topicId": 1,
						  "name": "이름",
						  "description": "설명"
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", is(400)));
	}

	@Test
	void updateTypeReturnsBadRequestWhenNameIsBlank() throws Exception {
		mockMvc.perform(put("/api/admin/types/{id}", 1)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "topicId": 1,
						  "name": "",
						  "description": "설명"
						}
						"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", is(400)))
				.andExpect(jsonPath("$.fieldErrors.name").exists());
	}

	@Test
	void updatedTypeIsVisibleFromStudentTypesApi() throws Exception {
		mockMvc.perform(put("/api/admin/types/{id}", 2)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "topicId": 1,
						  "name": "생활 상담",
						  "description": "학교 생활 관련 상담"
						}
						"""))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/types"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[1].id", is(2)))
				.andExpect(jsonPath("$[1].name", is("생활 상담")))
				.andExpect(jsonPath("$[1].description", is("학교 생활 관련 상담")));
	}

	@Test
	void deleteTypeReturnsNoContentWhenTypeIsUnused() throws Exception {
		String location = mockMvc.perform(post("/api/admin/types")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "topicId": 1,
						  "name": "삭제 대상",
						  "description": "참조 없음"
						}
						"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getHeader("Location");

		Long createdId = extractId(location);

		mockMvc.perform(delete("/api/admin/types/{id}", createdId))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/admin/types"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id")
						.value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem(createdId.intValue()))));
	}

	@Test
	void deleteTypeReturnsConflictWhenTypeIsReferenced() throws Exception {
		String location = mockMvc.perform(post("/api/admin/types")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "topicId": 1,
						  "name": "삭제 불가",
						  "description": "참조 있음"
						}
						"""))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getHeader("Location");

		Long createdId = extractId(location);

		Long studentSessionId = enterStudent("참조학생", "01034343434");
		mockMvc.perform(post("/api/consultations")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "studentSessionId": %d,
						  "typeId": %d
						}
						""".formatted(studentSessionId, createdId)))
				.andExpect(status().isCreated());

		mockMvc.perform(delete("/api/admin/types/{id}", createdId))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status", is(409)));
	}

	@Test
	void deleteTypeReturnsNotFoundWhenTypeDoesNotExist() throws Exception {
		Long missingId = consultationTypeRepository.findAll().stream()
				.mapToLong(consultationType -> consultationType.getId())
				.max()
				.orElse(5L) + 100L;

		mockMvc.perform(delete("/api/admin/types/{id}", missingId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status", is(404)));
	}

	private Long extractId(String location) {
		return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
	}

	private Long enterStudent(String studentName, String studentPhone) throws Exception {
		String location = mockMvc.perform(post("/api/students/enter")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "studentName": "%s",
						  "studentPhone": "%s",
						  "schoolType": "MIDDLE_SCHOOL",
						  "grade": 1
						}
						""".formatted(studentName, studentPhone)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getHeader("Location");

		return extractId(location);
	}
}
