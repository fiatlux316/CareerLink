package com.careerlink.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.careerlink.backend.dto.ConsultationTopicResponse;
import com.careerlink.backend.service.ConsultationTopicService;

@RestController
@RequestMapping("/api/topics")
public class ConsultationTopicController {

	private final ConsultationTopicService consultationTopicService;

	public ConsultationTopicController(ConsultationTopicService consultationTopicService) {
		this.consultationTopicService = consultationTopicService;
	}

	@GetMapping
	public List<ConsultationTopicResponse> getTopics() {
		return consultationTopicService.getAllTopics()
			.stream()
			.map(ConsultationTopicResponse::from)
			.toList();
	}
}
