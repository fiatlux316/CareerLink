package com.careerlink.backend;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
class CounselorApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ConsultationTypeRepository consultationTypeRepository;

	@Test
	void counselorCanEnterSuccessfully() throws Exception {
		Long typeId = consultationTypeRepository.findAll().get(0).getId();

		mockMvc.perform(post("/api/counselor/enter")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "counselorName": "상담사A",
					  "counselorPhone": "01012345678",
					  "typeId": %d
					}
					""".formatted(typeId)))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("/api/counselor/enter/")))
			.andExpect(jsonPath("$.counselorName", is("상담사A")))
			.andExpect(jsonPath("$.typeId", is(typeId.intValue())))
			.andExpect(jsonPath("$.typeName", is("고상해")));
	}

	@Test
	void counselorConsultationListReturnsOnlyRequestedTypeAndStatus() throws Exception {
		Long firstTypeId = consultationTypeRepository.findAll().get(0).getId();
		Long secondTypeId = consultationTypeRepository.findAll().get(1).getId();

		String acceptedLocation = createConsultation("학생1", "01011112222", firstTypeId);
		createConsultation("학생2", "01022223333", firstTypeId);
		createConsultation("학생3", "01033334444", secondTypeId);

		mockMvc.perform(patch(acceptedLocation + "/accept")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "counselorName": "상담사B"
					}
					"""))
			.andExpect(status().isOk());

		mockMvc.perform(get("/api/counselor/consultations")
				.param("typeId", String.valueOf(firstTypeId))
				.param("status", "RECEIVED"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].studentName", is("학생2")))
			.andExpect(jsonPath("$[0].typeId", is(firstTypeId.intValue())))
			.andExpect(jsonPath("$[0].status", is("RECEIVED")));
	}

	@Test
	void acceptedConsultationsFilteredByCounselorName() throws Exception {
		Long typeId = consultationTypeRepository.findAll().get(0).getId();
		String loc1 = createConsultation("학생A", "01011111111", typeId);
		String loc2 = createConsultation("학생B", "01022222222", typeId);

		mockMvc.perform(patch(loc1 + "/accept")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "counselorName": "상담사1"
					}
					"""))
			.andExpect(status().isOk());

		mockMvc.perform(patch(loc2 + "/accept")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "counselorName": "상담사2"
					}
					"""))
			.andExpect(status().isOk());

		mockMvc.perform(get("/api/counselor/consultations")
				.param("typeId", String.valueOf(typeId))
				.param("status", "ACCEPTED")
				.param("counselorName", "상담사1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(1)))
			.andExpect(jsonPath("$[0].studentName", is("학생A")))
			.andExpect(jsonPath("$[0].counselorName", is("상담사1")));
	}

	@Test
	void acceptConsultationChangesStatusToAccepted() throws Exception {
		Long typeId = consultationTypeRepository.findAll().get(0).getId();
		String location = createConsultation("학생4", "01044445555", typeId);

		mockMvc.perform(patch(location + "/accept")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "counselorName": "상담사C"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is("ACCEPTED")))
			.andExpect(jsonPath("$.counselorName", is("상담사C")));
	}

	@Test
	void cancelAcceptConsultationRevertsStatusToReceived() throws Exception {
		Long typeId = consultationTypeRepository.findAll().get(0).getId();
		String location = createConsultation("학생X", "01099998888", typeId);

		mockMvc.perform(patch(location + "/accept")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "counselorName": "상담사X"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is("ACCEPTED")));

		mockMvc.perform(patch(location + "/cancel-accept"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is("RECEIVED")))
			.andExpect(jsonPath("$.counselorName").value(org.hamcrest.Matchers.nullValue()));
	}

	@Test
	void acceptConsultationReturnsConflictWhenRetried() throws Exception {
		Long typeId = consultationTypeRepository.findAll().get(0).getId();
		String location = createConsultation("학생5", "01055556666", typeId);

		mockMvc.perform(patch(location + "/accept")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "counselorName": "상담사D"
					}
					"""))
			.andExpect(status().isOk());

		mockMvc.perform(patch(location + "/accept")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "counselorName": "상담사E"
					}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status", is(409)));
	}

	@Test
	void completeConsultationChangesStatusToCompleted() throws Exception {
		Long typeId = consultationTypeRepository.findAll().get(0).getId();
		String location = createConsultation("학생6", "01066667777", typeId);

		mockMvc.perform(patch(location + "/accept")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{
					  "counselorName": "상담사F"
					}
					"""))
			.andExpect(status().isOk());

		mockMvc.perform(patch(location + "/start-progress"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is("IN_PROGRESS")));

		mockMvc.perform(patch(location + "/complete"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status", is("COMPLETED")));
	}

	@Test
	void completeConsultationReturnsConflictWhenStatusIsReceived() throws Exception {
		Long typeId = consultationTypeRepository.findAll().get(0).getId();
		String location = createConsultation("학생7", "01077778888", typeId);

		mockMvc.perform(patch(location + "/complete"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status", is(409)));
	}

	private String createConsultation(String studentName, String studentPhone, Long typeId) throws Exception {
		Long studentSessionId = enterStudent(studentName, studentPhone);
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

		return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
	}
}
