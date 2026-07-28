package com.careerlink.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class PostgresIntegrationTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
		.withDatabaseName("careerlink")
		.withUsername("careerlink")
		.withPassword("careerlink");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
	}

	@Autowired
	private TestRestTemplate restTemplate;

	@LocalServerPort
	private int port;

	@Test
	void fullStudentCounselorFlowRunsOnPostgres() {
		ResponseEntity<List<ConsultationTypeResponse>> typesResponse = restTemplate.exchange(
			url("/api/types"),
			HttpMethod.GET,
			null,
			new ParameterizedTypeReference<>() {
			}
		);

		assertThat(typesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(typesResponse.getBody()).hasSizeGreaterThanOrEqualTo(2);

		Long primaryTypeId = typesResponse.getBody().get(0).id();
		Long secondaryTypeId = typesResponse.getBody().get(1).id();

		ConsultationResponse cancellableConsultation = createConsultation("포스트그레스학생1", "01012345678", primaryTypeId);
		ConsultationResponse cancelledConsultation = restTemplate.exchange(
			url("/api/consultations/" + cancellableConsultation.id() + "/cancel"),
			HttpMethod.PATCH,
			HttpEntity.EMPTY,
			ConsultationResponse.class
		).getBody();

		assertThat(cancelledConsultation.status()).isEqualTo("CANCELLED");

		ResponseEntity<CounselorSessionResponse> counselorEnterResponse = restTemplate.postForEntity(
			url("/api/counselor/enter"),
			jsonRequest(Map.of(
				"counselorName", "상담사포스트그레스",
				"counselorPhone", "01087654321",
				"typeId", primaryTypeId
			)),
			CounselorSessionResponse.class
		);

		assertThat(counselorEnterResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(counselorEnterResponse.getBody().typeId()).isEqualTo(primaryTypeId);

		ConsultationResponse receivableConsultation = createConsultation("포스트그레스학생2", "01011112222", primaryTypeId);
		createConsultation("포스트그레스학생3", "01011112222", secondaryTypeId);

		ResponseEntity<List<ConsultationResponse>> waitingConsultationsResponse = restTemplate.exchange(
			url("/api/counselor/consultations?typeId=" + primaryTypeId + "&status=RECEIVED"),
			HttpMethod.GET,
			null,
			new ParameterizedTypeReference<>() {
			}
		);

		assertThat(waitingConsultationsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(waitingConsultationsResponse.getBody())
			.extracting(ConsultationResponse::id)
			.contains(receivableConsultation.id())
			.doesNotContain(cancelledConsultation.id());

		ConsultationResponse acceptedConsultation = restTemplate.exchange(
			url("/api/consultations/" + receivableConsultation.id() + "/accept"),
			HttpMethod.PATCH,
			jsonRequest(Map.of("counselorName", "상담사포스트그레스")),
			ConsultationResponse.class
		).getBody();

		assertThat(acceptedConsultation.status()).isEqualTo("IN_PROGRESS");
		assertThat(acceptedConsultation.counselorName()).isEqualTo("상담사포스트그레스");

		ConsultationResponse completedConsultation = restTemplate.exchange(
			url("/api/consultations/" + receivableConsultation.id() + "/complete"),
			HttpMethod.PATCH,
			HttpEntity.EMPTY,
			ConsultationResponse.class
		).getBody();

		assertThat(completedConsultation.status()).isEqualTo("COMPLETED");

		ConsultationResponse fetchedConsultation = restTemplate.getForObject(
			url("/api/consultations/" + receivableConsultation.id()),
			ConsultationResponse.class
		);

		assertThat(fetchedConsultation.status()).isEqualTo("COMPLETED");
	}

	@Test
	void adminTypeCreateAndDeleteRulesRunOnPostgres() {
		ResponseEntity<ConsultationTypeResponse> createTypeResponse = restTemplate.postForEntity(
			url("/api/admin/types"),
			jsonRequest(Map.of(
				"name", "포스트그레스 신규 유형",
				"description", "포스트그레스 기반 생성"
			)),
			ConsultationTypeResponse.class
		);

		assertThat(createTypeResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Long deletableTypeId = createTypeResponse.getBody().id();

		ResponseEntity<Void> deleteUnusedResponse = restTemplate.exchange(
			url("/api/admin/types/" + deletableTypeId),
			HttpMethod.DELETE,
			HttpEntity.EMPTY,
			Void.class
		);

		assertThat(deleteUnusedResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<ConsultationTypeResponse> inUseTypeResponse = restTemplate.postForEntity(
			url("/api/admin/types"),
			jsonRequest(Map.of(
				"name", "삭제불가 유형",
				"description", "참조 데이터 존재"
			)),
			ConsultationTypeResponse.class
		);

		assertThat(inUseTypeResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Long inUseTypeId = inUseTypeResponse.getBody().id();

		createConsultation("참조학생", "01099998888", inUseTypeId);

		ResponseEntity<ErrorResponse> deleteInUseResponse = restTemplate.exchange(
			url("/api/admin/types/" + inUseTypeId),
			HttpMethod.DELETE,
			HttpEntity.EMPTY,
			ErrorResponse.class
		);

		assertThat(deleteInUseResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(deleteInUseResponse.getBody().message()).contains("consultationCount=1");
	}

	private ConsultationResponse createConsultation(String studentName, String studentPhone, Long typeId) {
		ResponseEntity<ConsultationResponse> response = restTemplate.postForEntity(
			url("/api/consultations"),
			jsonRequest(Map.of(
				"studentName", studentName,
				"studentPhone", studentPhone,
				"typeId", typeId
			)),
			ConsultationResponse.class
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		return response.getBody();
	}

	private HttpEntity<Map<String, Object>> jsonRequest(Map<String, Object> body) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return new HttpEntity<>(body, headers);
	}

	private String url(String path) {
		return "http://localhost:" + port + path;
	}

	private record ConsultationTypeResponse(Long id, String name, String description) {
	}

	private record CounselorSessionResponse(Long id, String counselorName, Long typeId, String typeName, String enteredAt) {
	}

	private record ConsultationResponse(
		Long id,
		String studentName,
		String studentPhone,
		Long typeId,
		String typeName,
		String status,
		String counselorName,
		String createdAt,
		String updatedAt
	) {
	}

	private record ErrorResponse(int status, String message, String timestamp, Map<String, String> fieldErrors) {
	}
}
