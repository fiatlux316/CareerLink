package com.careerlink.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.careerlink.backend.controller.AdminController;
import com.careerlink.backend.domain.SchoolType;
import com.careerlink.backend.domain.StudentSession;
import com.careerlink.backend.dto.StudentSessionResponse;
import com.careerlink.backend.dto.StudentSessionUpdateRequest;
import com.careerlink.backend.repository.StudentSessionRepository;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class AdminStudentManagementIntegrationTest {

	@Autowired
	private AdminController adminController;

	@Autowired
	private StudentSessionRepository studentSessionRepository;

	@Test
	void getAllStudentsReturnsStudentList() {
		StudentSession session1 = studentSessionRepository.save(
			new StudentSession("홍길동", "010-1111-2222", SchoolType.HIGH_SCHOOL, 2, 1)
		);
		StudentSession session2 = studentSessionRepository.save(
			new StudentSession("김철수", "010-3333-4444", SchoolType.MIDDLE_SCHOOL, 1, 1)
		);

		List<StudentSessionResponse> students = adminController.getAllStudents();

		assertThat(students).hasSizeGreaterThanOrEqualTo(2);
		assertThat(students).extracting(StudentSessionResponse::studentName)
			.contains("홍길동", "김철수");
	}

	@Test
	void updateStudentUpdatesAllEditableStudentSessionFields() {
		StudentSession original = studentSessionRepository.save(
			new StudentSession("기존학생", "010-0000-0000", SchoolType.MIDDLE_SCHOOL, 1, 0)
		);

		StudentSessionUpdateRequest updateRequest = new StudentSessionUpdateRequest(
			"수정학생",
			"010-9999-8888",
			SchoolType.HIGH_SCHOOL,
			3,
			2
		);

		StudentSessionResponse response = adminController.updateStudent(original.getId(), updateRequest);

		assertThat(response.id()).isEqualTo(original.getId());
		assertThat(response.studentName()).isEqualTo("수정학생");
		assertThat(response.studentPhone()).isEqualTo("010-9999-8888");
		assertThat(response.schoolType()).isEqualTo(SchoolType.HIGH_SCHOOL);
		assertThat(response.grade()).isEqualTo(3);
		assertThat(response.gender()).isEqualTo(2);

		StudentSession updated = studentSessionRepository.findById(original.getId()).orElseThrow();
		assertThat(updated.getStudentName()).isEqualTo("수정학생");
		assertThat(updated.getStudentPhone()).isEqualTo("010-9999-8888");
		assertThat(updated.getSchoolType()).isEqualTo(SchoolType.HIGH_SCHOOL);
		assertThat(updated.getGrade()).isEqualTo(3);
		assertThat(updated.getGender()).isEqualTo(2);
	}
}
