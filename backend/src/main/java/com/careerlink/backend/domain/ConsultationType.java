package com.careerlink.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@Table(name = "consultation_types")
public class ConsultationType {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "topic_id")
	private ConsultationTopic topic;

	@Column(nullable = false)
	private String name;

	@Column(columnDefinition = "TEXT", nullable = false)
	private String description;

	protected ConsultationType() {
	}

	public ConsultationType(ConsultationTopic topic, String name, String description) {
		this.topic = topic;
		this.name = name;
		this.description = description;
	}

	public ConsultationType(String name, String description) {
		this.name = name;
		this.description = description;
	}

	public Long getId() {
		return id;
	}

	public ConsultationTopic getTopic() {
		return topic;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public void update(ConsultationTopic topic, String name, String description) {
		this.topic = topic;
		this.name = name;
		this.description = description;
	}

	public void update(String name, String description) {
		this.name = name;
		this.description = description;
	}
}
