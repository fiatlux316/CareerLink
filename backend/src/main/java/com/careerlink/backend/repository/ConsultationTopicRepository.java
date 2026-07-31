package com.careerlink.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.careerlink.backend.domain.ConsultationTopic;

public interface ConsultationTopicRepository extends JpaRepository<ConsultationTopic, Long> {
	Optional<ConsultationTopic> findByName(String name);
}
