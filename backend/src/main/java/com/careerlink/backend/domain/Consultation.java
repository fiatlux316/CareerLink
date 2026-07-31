package com.careerlink.backend.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import com.careerlink.backend.exception.InvalidStatusTransitionException;

@Entity
@Table(name = "consultations")
public class Consultation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "student_id", nullable = false)
	private StudentSession studentSession;

	@ManyToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "type_id", nullable = false)
	private ConsultationType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@JdbcTypeCode(SqlTypes.VARCHAR)
	private ConsultationStatus status = ConsultationStatus.RECEIVED;

	private String counselorName;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	private LocalDateTime maskedAt;

	protected Consultation() {
	}

	public Consultation(StudentSession studentSession, ConsultationType type) {
		this.studentSession = studentSession;
		this.type = type;
	}

	@PrePersist
	void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		createdAt = now;
		updatedAt = now;
		if (status == null) {
			status = ConsultationStatus.RECEIVED;
		}
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getStudentName() {
		return studentSession.getStudentName();
	}

	public String getStudentPhone() {
		return studentSession.getStudentPhone();
	}

	public StudentSession getStudentSession() {
		return studentSession;
	}

	public ConsultationType getType() {
		return type;
	}

	public ConsultationStatus getStatus() {
		return status;
	}

	public String getCounselorName() {
		return counselorName;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public LocalDateTime getMaskedAt() {
		return maskedAt;
	}

	public void accept(String counselorName) {
		validateTransition(ConsultationStatus.ACCEPTED);
		this.counselorName = counselorName;
		this.status = ConsultationStatus.ACCEPTED;
	}

	public void cancelAccept() {
		validateTransition(ConsultationStatus.RECEIVED);
		this.status = ConsultationStatus.RECEIVED;
		this.counselorName = null;
	}

	public void startProgress() {
		validateTransition(ConsultationStatus.IN_PROGRESS);
		this.status = ConsultationStatus.IN_PROGRESS;
	}

	public void complete() {
		validateTransition(ConsultationStatus.COMPLETED);
		this.status = ConsultationStatus.COMPLETED;
	}

	public void cancel() {
		validateTransition(ConsultationStatus.CANCELLED);
		this.status = ConsultationStatus.CANCELLED;
	}

	public void maskSensitiveData(LocalDateTime maskedAt) {
		if (this.maskedAt != null) {
			return;
		}

		// 주의: 같은 StudentSession을 참조하는 다른 Consultation이 있다면, 한 쪽이 마스킹될 때 함께 마스킹된 것처럼
		// 보이는 부작용이 있을 수 있다(StudentSession 컴멘트 참고). 다음 태스크에서 정책 재검토 필요.
		this.studentSession.maskSensitiveData();
		this.maskedAt = maskedAt;
	}

	private void validateTransition(ConsultationStatus targetStatus) {
		boolean allowed = switch (status) {
			case RECEIVED -> targetStatus == ConsultationStatus.ACCEPTED
				|| targetStatus == ConsultationStatus.CANCELLED;
			case ACCEPTED -> targetStatus == ConsultationStatus.IN_PROGRESS
				|| targetStatus == ConsultationStatus.RECEIVED;
			case IN_PROGRESS -> targetStatus == ConsultationStatus.COMPLETED;
			case COMPLETED -> false;
			case CANCELLED -> false;
		};

		if (!allowed) {
			throw new InvalidStatusTransitionException(status, targetStatus);
		}
	}
}
