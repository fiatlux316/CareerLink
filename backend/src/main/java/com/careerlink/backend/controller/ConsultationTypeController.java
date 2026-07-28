package com.careerlink.backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.careerlink.backend.dto.ConsultationTypeResponse;
import com.careerlink.backend.service.ConsultationTypeService;

@RestController
@RequestMapping("/api/types")
public class ConsultationTypeController {

	private final ConsultationTypeService consultationTypeService;

	public ConsultationTypeController(ConsultationTypeService consultationTypeService) {
		this.consultationTypeService = consultationTypeService;
	}

	@GetMapping
	public List<ConsultationTypeResponse> getTypes() {
		return consultationTypeService.getAllTypes()
			.stream()
			.map(ConsultationTypeResponse::from)
			.toList();
	}
}
