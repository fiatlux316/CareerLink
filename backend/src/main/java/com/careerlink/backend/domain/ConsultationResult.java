package com.careerlink.backend.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "consultation_results")
public class ConsultationResult {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "consultation_id", nullable = false, unique = true)
	private Consultation consultation;

	@Column(name = "result_content", nullable = false, length = 100)
	private String resultContent;

	@Column(name = "re_consultation_needed", nullable = false)
	private Integer reConsultationNeeded; // 1: 필요, 2: 불필요

	@Column(name = "satisfaction_score", nullable = false)
	private Integer satisfactionScore; // 1~5점

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	protected ConsultationResult() {
	}

	public ConsultationResult(Consultation consultation, String resultContent, Integer reConsultationNeeded, Integer satisfactionScore) {
		this.consultation = consultation;
		this.resultContent = resultContent;
		this.reConsultationNeeded = reConsultationNeeded != null ? reConsultationNeeded : 2;
		this.satisfactionScore = satisfactionScore != null ? satisfactionScore : 5;
	}

	@PrePersist
	void prePersist() {
		if (createdAt == null) {
			createdAt = LocalDateTime.now();
		}
		if (reConsultationNeeded == null) {
			reConsultationNeeded = 2;
		}
		if (satisfactionScore == null) {
			satisfactionScore = 5;
		}
	}

	public Long getId() {
		return id;
	}

	public Consultation getConsultation() {
		return consultation;
	}

	public String getResultContent() {
		return resultContent;
	}

	public Integer getReConsultationNeeded() {
		return reConsultationNeeded;
	}

	public Integer getSatisfactionScore() {
		return satisfactionScore;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
