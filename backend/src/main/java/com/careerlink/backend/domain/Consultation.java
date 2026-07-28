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

	@Column(nullable = false)
	private String studentName;

	@Column(nullable = false)
	private String studentPhone;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
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

	public Consultation(String studentName, String studentPhone, ConsultationType type) {
		this.studentName = studentName;
		this.studentPhone = studentPhone;
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
		return studentName;
	}

	public String getStudentPhone() {
		return studentPhone;
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
		validateTransition(ConsultationStatus.IN_PROGRESS);
		this.counselorName = counselorName;
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

		this.studentName = "***";
		this.studentPhone = "***-****-****";
		this.maskedAt = maskedAt;
	}

	private void validateTransition(ConsultationStatus targetStatus) {
		boolean allowed = switch (status) {
			case RECEIVED -> targetStatus == ConsultationStatus.IN_PROGRESS
				|| targetStatus == ConsultationStatus.CANCELLED;
			case CANCELLED -> false;
			case IN_PROGRESS -> targetStatus == ConsultationStatus.COMPLETED;
			case COMPLETED -> false;
		};

		if (!allowed) {
			throw new InvalidStatusTransitionException(status, targetStatus);
		}
	}
}
