package com.careerlink.backend.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.careerlink.backend.dto.ConsultationResponse;
import com.careerlink.backend.dto.ConsultationTypeCreateRequest;
import com.careerlink.backend.dto.ConsultationTypeResponse;
import com.careerlink.backend.dto.ConsultationTypeUpdateRequest;
import com.careerlink.backend.service.ConsultationService;
import com.careerlink.backend.service.ConsultationTypeService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Validated
@RestController
@RequestMapping("/api/admin")
public class AdminController {

	private final ConsultationTypeService consultationTypeService;
	private final ConsultationService consultationService;

	public AdminController(ConsultationTypeService consultationTypeService, ConsultationService consultationService) {
		this.consultationTypeService = consultationTypeService;
		this.consultationService = consultationService;
	}

	@GetMapping("/types")
	public List<ConsultationTypeResponse> getTypes() {
		return consultationTypeService.getAllTypes()
			.stream()
			.map(ConsultationTypeResponse::from)
			.toList();
	}

	@PostMapping("/types")
	public ResponseEntity<ConsultationTypeResponse> createType(@Valid @RequestBody ConsultationTypeCreateRequest request) {
		ConsultationTypeResponse response = ConsultationTypeResponse.from(
			consultationTypeService.createType(request.name(), request.description())
		);

		return ResponseEntity
			.created(URI.create("/api/admin/types/" + response.id()))
			.body(response);
	}

	@PutMapping("/types/{id}")
	public ConsultationTypeResponse updateType(
		@PathVariable @Positive(message = "id는 1 이상이어야 합니다.") Long id,
		@Valid @RequestBody ConsultationTypeUpdateRequest request
	) {
		return ConsultationTypeResponse.from(
			consultationTypeService.updateType(id, request.name(), request.description())
		);
	}

	@DeleteMapping("/types/{id}")
	public ResponseEntity<Void> deleteType(@PathVariable @Positive(message = "id는 1 이상이어야 합니다.") Long id) {
		consultationTypeService.deleteType(id);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/consultations")
	public List<ConsultationResponse> getAllConsultations() {
		return consultationService.getAllConsultations()
			.stream()
			.map(ConsultationResponse::from)
			.toList();
	}
}

