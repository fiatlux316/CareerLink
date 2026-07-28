package com.careerlink.backend.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "counselor_sessions")
public class CounselorSession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String counselorName;

	@Column(nullable = true)
	private String counselorPhone;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "type_id", nullable = false)
	private ConsultationType type;

	@Column(nullable = false)
	private LocalDateTime enteredAt;

	protected CounselorSession() {
	}

	public CounselorSession(String counselorName, String counselorPhone, ConsultationType type) {
		this.counselorName = counselorName;
		this.counselorPhone = counselorPhone;
		this.type = type;
	}

	@PrePersist
	void prePersist() {
		enteredAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public String getCounselorName() {
		return counselorName;
	}

	public String getCounselorPhone() {
		return counselorPhone;
	}

	public ConsultationType getType() {
		return type;
	}

	public LocalDateTime getEnteredAt() {
		return enteredAt;
	}
}
