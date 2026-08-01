package com.careerlink.backend.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_sessions")
public class StudentSession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "student_name", nullable = false)
	private String studentName;

	@Column(name = "student_phone", nullable = false)
	private String studentPhone;

	@Enumerated(EnumType.STRING)
	@Column(name = "school_type", nullable = false)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	private SchoolType schoolType;

	@Column(name = "grade", nullable = false, columnDefinition = "integer default 0")
	private Integer grade = 0;

	@Column(name = "gender", nullable = false, columnDefinition = "integer default 0")
	private Integer gender = 0;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "entered_at", nullable = false)
	private LocalDateTime enteredAt;

	protected StudentSession() {
	}

	public StudentSession(String studentName, String studentPhone, SchoolType schoolType, Integer grade) {
		this(studentName, studentPhone, schoolType, grade, 0);
	}

	public StudentSession(String studentName, String studentPhone, SchoolType schoolType, Integer grade, Integer gender) {
		this.studentName = studentName;
		this.studentPhone = studentPhone;
		this.schoolType = schoolType;
		this.grade = grade != null ? grade : 0;
		this.gender = gender != null ? gender : 0;
	}

	public void updateInfo(String studentName, String studentPhone, SchoolType schoolType, Integer grade, Integer gender) {
		this.studentName = studentName;
		this.studentPhone = studentPhone;
		this.schoolType = schoolType;
		this.grade = grade != null ? grade : 0;
		this.gender = gender != null ? gender : 0;
	}

	@PrePersist
	void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.enteredAt = now;
		if (this.grade == null) {
			this.grade = 0;
		}
		if (this.gender == null) {
			this.gender = 0;
		}
	}

	public Long getId() {
		return id;
	}

	public String getStudentName() {
		return studentName;
	}

	public String getStudentPhone() {
		return studentPhone;
	}

	public SchoolType getSchoolType() {
		return schoolType;
	}

	public Integer getGrade() {
		return grade != null ? grade : 0;
	}

	public Integer getGender() {
		return gender != null ? gender : 0;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt != null ? createdAt : enteredAt;
	}

	public LocalDateTime getEnteredAt() {
		return enteredAt != null ? enteredAt : createdAt;
	}

	// 주의: 여러 Consultation이 동일 StudentSession을 참조할 수 있어(한 학생이 여러 유형에 신청),
	// 특정 Consultation이 마스킹되어 이 StudentSession이 마스킹되면
	// 같은 세션을 참조하는 아직 보관기간이 지나지 않은 다른 Consultation도 이름이 마스킹된 것처럼
	// 보이는 부작용이 있을 수 있다. 이번 태스크 범위에서는 기존 동작(Consultation 단위 마스킹)과
	// 최대한 유사하게 구현하며, 이 정책은 다음 태스크에서 재검토가 필요하다.
	public void maskSensitiveData() {
		this.studentName = "***";
		this.studentPhone = "***-****-****";
	}
}
