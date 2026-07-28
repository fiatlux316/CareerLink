package com.careerlink.backend;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.careerlink.backend.domain.ConsultationType;
import com.careerlink.backend.repository.ConsultationTypeRepository;

@SpringBootApplication
public class CareerlinkBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CareerlinkBackendApplication.class, args);
	}

	@Bean
	CommandLineRunner consultationTypeSeeder(ConsultationTypeRepository consultationTypeRepository) {
		return args -> {
			if (consultationTypeRepository.count() > 0) {
				return;
			}

			consultationTypeRepository.saveAll(List.of(
				new ConsultationType("상담유형1", "기본 상담유형 설명 1"),
				new ConsultationType("상담유형2", "기본 상담유형 설명 2"),
				new ConsultationType("상담유형3", "기본 상담유형 설명 3"),
				new ConsultationType("상담유형4", "기본 상담유형 설명 4"),
				new ConsultationType("상담유형5", "기본 상담유형 설명 5")));
		};
	}

}
