package com.careerlink.backend;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.careerlink.backend.domain.ConsultationTopic;
import com.careerlink.backend.domain.ConsultationType;
import com.careerlink.backend.repository.ConsultationTopicRepository;
import com.careerlink.backend.repository.ConsultationTypeRepository;

@SpringBootApplication
public class CareerlinkBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(CareerlinkBackendApplication.class, args);
	}

	@Bean
	CommandLineRunner consultationTypeSeeder(
			ConsultationTopicRepository consultationTopicRepository,
			ConsultationTypeRepository consultationTypeRepository) {
		return args -> {
			ConsultationTopic topicSelf = consultationTopicRepository.findByName("자기이해")
					.orElseGet(() -> consultationTopicRepository
							.save(new ConsultationTopic("자기이해", "나를 더욱 깊이 이해하는 상담 테마")));
			ConsultationTopic topicCareer = consultationTopicRepository.findByName("학과/직업")
					.orElseGet(() -> consultationTopicRepository
							.save(new ConsultationTopic("학과/직업", "학과 및 직업 탐색 관련 상담 테마")));

			if (consultationTypeRepository.count() > 0) {
				List<ConsultationType> existingTypes = consultationTypeRepository.findAll();
				for (ConsultationType type : existingTypes) {
					if (type.getTopic() == null) {
						type.update(topicSelf, type.getName(), type.getDescription());
						consultationTypeRepository.save(type);
					}
				}
				return;
			}

			consultationTypeRepository.saveAll(List.of(
					new ConsultationType(topicSelf, "고상해", "기본 자기이해 상담유형 1"),
					new ConsultationType(topicSelf, "수비학", "기본 자기이해 상담유형 2"),
					new ConsultationType(topicSelf, "타로", "기본 자기이해 상담유형 3"),
					new ConsultationType(topicCareer, "학과탐색", "기본 학과&직업 상담유형 1"),
					new ConsultationType(topicCareer, "직업적성", "기본 학과&직업 상담유형 2")));
		};
	}
}
