package com.careerlink.backend;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import com.careerlink.backend.repository.ConsultationTypeRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.jpa.open-in-view=true")
@Transactional
class StudentApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ConsultationTypeRepository consultationTypeRepository;

	@Test
	void getTypesReturnsSeededConsultationTypes() throws Exception {
		mockMvc.perform(get("/api/types"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(5)))
			.andExpect(jsonPath("$[0].name", is("상담유형1")));
	}

	@Test
	void enterStudentReturnsCreatedStudentSession() throws Exception {
		mockMvc.perform(post("/api/students/enter")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "studentName": "홍길동",
					  "studentPhone": "01012345678",
					  "schoolType": "MIDDLE_SCHOOL",
					  "grade": 2
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/students/enter/")))
			.andExpect(jsonPath("$.studentName", is("홍길동")))
			.andExpect(jsonPath("$.studentPhone", is("01012345678")))
			.andExpect(jsonPath("$.schoolType", is("MIDDLE_SCHOOL")))
			.andExpect(jsonPath("$.grade", is(2)));
	}

	@Test
	void enterStudentReturnsBadRequestWhenSchoolTypeIsInvalid() throws Exception {
		mockMvc.perform(post("/api/students/enter")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "studentName": "홍길동",
					  "studentPhone": "01012345678",
					  "schoolType": "INVALID_TYPE",
					  "grade": 2
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status", is(400)));
	}

	@Test
	void enterStudentReturnsBadRequestWhenGradeIsOutOfRange() throws Exception {
		mockMvc.perform(post("/api/students/enter")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "studentName": "홍길동",
					  "studentPhone": "01012345678",
					  "schoolType": "HIGH_SCHOOL",
					  "grade": 4
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status", is(400)))
			.andExpect(jsonPath("$.fieldErrors.grade").exists());
	}

	@Test
	void enterStudentReturnsBadRequestWhenRequiredFieldsAreMissing() throws Exception {
		mockMvc.perform(post("/api/students/enter")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "studentName": "",
					  "studentPhone": "010-12-3456"
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status", is(400)))
			.andExpect(jsonPath("$.fieldErrors.studentName").exists())
			.andExpect(jsonPath("$.fieldErrors.studentPhone").exists())
			.andExpect(jsonPath("$.fieldErrors.schoolType").exists())
			.andExpect(jsonPath("$.fieldErrors.grade").exists());
	}

	@Test
	void createConsultationReturnsCreatedConsultation() throws Exception {
		Long typeId = consultationTypeRepository.findAll().get(0).getId();
		Long studentSessionId = enterStudent("홍길동", "01012345678");

		mockMvc.perform(post("/api/consultations")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "studentSessionId": %d,
					  "typeId": %d
					}
					""".formatted(studentSessionId, typeId)))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/consultations/")))
			.andExpect(jsonPath("$.studentName", is("홍길동")))
			.andExpect(jsonPath("$.studentPhone", is("01012345678")))
			.andExpect(jsonPath("$.typeId", is(typeId.intValue())))
			.andExpect(jsonPath("$.typeName", is("상담유형1")))
			.andExpect(jsonPath("$.status", is("RECEIVED")));
	}

	@Test
	void createConsultationReturnsBadRequestWhenInputIsInvalid() throws Exception {
		mockMvc.perform(post("/api/consultations")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status", is(400)))
			.andExpect(jsonPath("$.fieldErrors.studentSessionId").exists())
			.andExpect(jsonPath("$.fieldErrors.typeId").exists());
	}

	@Test
	void createConsultationReturnsNotFoundWhenTypeDoesNotExist() throws Exception {
		Long studentSessionId = enterStudent("홍길동", "01012345678");

		mockMvc.perform(post("/api/consultations")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "studentSessionId": %d,
					  "typeId": 999999
					}
					""".formatted(studentSessionId)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status", is(404)));
	}

	@Test
	void createConsultationReturnsNotFoundWhenStudentSessionDoesNotExist() throws Exception {
		Long typeId = consultationTypeRepository.findAll().get(0).getId();

		mockMvc.perform(post("/api/consultations")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "studentSessionId": 999999,
					  "typeId": %d
					}
					""".formatted(typeId)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status", is(404)));
	}

	@Test
	void getConsultationReturnsConsultationWhenItExists() throws Exception {
		Long typeId = consultationTypeRepository.findAll().get(0).getId();
		Long studentSessionId = enterStudent("김학생", "010-1234-5678");

		String location = mockMvc.perform(post("/api/consultations")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "studentSessionId": %d,
					  "typeId": %d
					}
					""".formatted(studentSessionId, typeId)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getHeader("Location");

		mockMvc.perform(get(location))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.studentName", is("김학생")))
			.andExpect(jsonPath("$.status", is("RECEIVED")));
	}

	@Test
	void getConsultationReturnsNotFoundWhenItDoesNotExist() throws Exception {
		mockMvc.perform(get("/api/consultations/{id}", 999999L))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status", is(404)));
	}

	@Test
	void cancelConsultationReturnsCancelledWhenStatusIsReceived() throws Exception {
		Long typeId = consultationTypeRepository.findAll().get(0).getId();
		String location = createConsultation("취소학생", "01044445555", typeId);

		mockMvc.perform(patch(location + "/cancel"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is("CANCELLED")));
	}

	@Test
	void cancelConsultationReturnsConflictWhenStatusIsInProgress() throws Exception {
		Long typeId = consultationTypeRepository.findAll().get(0).getId();
		String location = createConsultation("진행학생", "01055556666", typeId);

		mockMvc.perform(patch(location + "/accept")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "counselorName": "상담사A"
					}
					"""))
			.andExpect(status().isOk());

		mockMvc.perform(patch(location + "/cancel"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status", is(409)));
	}

	@Test
	void getConsultationsByStudentPhoneReturnsAllConsultationsInLatestOrder() throws Exception {
		Long firstTypeId = consultationTypeRepository.findAll().get(0).getId();
		Long secondTypeId = consultationTypeRepository.findAll().get(1).getId();
		Long studentSessionId = enterStudent("다건학생", "01066667777");

		String firstLocation = createConsultationForSession(studentSessionId, firstTypeId);
		String secondLocation = createConsultationForSession(studentSessionId, secondTypeId);
		createConsultation("다른학생", "01099990000", firstTypeId);

		mockMvc.perform(get("/api/consultations")
				.param("studentPhone", "01066667777"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].studentPhone", is("01066667777")))
			.andExpect(jsonPath("$[1].studentPhone", is("01066667777")))
			.andExpect(jsonPath("$[0].typeId", is(secondTypeId.intValue())))
			.andExpect(jsonPath("$[1].typeId", is(firstTypeId.intValue())))
			.andExpect(jsonPath("$[0].id", greaterThan(extractId(firstLocation))))
			.andExpect(jsonPath("$[1].id", is(extractId(firstLocation))));
	}

	@Test
	void getConsultationsByStudentPhoneReturnsBadRequestWhenParameterIsMissing() throws Exception {
		mockMvc.perform(get("/api/consultations"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status", is(400)));
	}

	@Test
	void sameStudentCanCreateMultipleConsultationsForDifferentTypes() throws Exception {
		Long firstTypeId = consultationTypeRepository.findAll().get(0).getId();
		Long secondTypeId = consultationTypeRepository.findAll().get(1).getId();
		Long studentSessionId = enterStudent("복수신청학생", "01012121212");

		mockMvc.perform(post("/api/consultations")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "studentSessionId": %d,
					  "typeId": %d
					}
					""".formatted(studentSessionId, firstTypeId)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.typeId", is(firstTypeId.intValue())));

		mockMvc.perform(post("/api/consultations")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "studentSessionId": %d,
					  "typeId": %d
					}
					""".formatted(studentSessionId, secondTypeId)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.typeId", is(secondTypeId.intValue())));

		mockMvc.perform(get("/api/consultations")
				.param("studentPhone", "01012121212"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)));
	}

	private String createConsultation(String studentName, String studentPhone, Long typeId) throws Exception {
		Long studentSessionId = enterStudent(studentName, studentPhone);
		return createConsultationForSession(studentSessionId, typeId);
	}

	private String createConsultationForSession(Long studentSessionId, Long typeId) throws Exception {
		return mockMvc.perform(post("/api/consultations")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "studentSessionId": %d,
					  "typeId": %d
					}
					""".formatted(studentSessionId, typeId)))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getHeader("Location");
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

		return (long) extractId(location);
	}

	private int extractId(String location) {
		return Integer.parseInt(location.substring(location.lastIndexOf('/') + 1));
	}
}
