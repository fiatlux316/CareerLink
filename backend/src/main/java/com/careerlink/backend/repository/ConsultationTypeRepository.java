package com.careerlink.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.careerlink.backend.domain.ConsultationType;

public interface ConsultationTypeRepository extends JpaRepository<ConsultationType, Long> {
}
