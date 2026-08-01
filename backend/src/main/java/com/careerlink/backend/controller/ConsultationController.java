package com.careerlink.backend.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.careerlink.backend.domain.Consultation;
import com.careerlink.backend.domain.ConsultationResult;
import com.careerlink.backend.dto.AcceptRequest;
import com.careerlink.backend.dto.ConsultationCompleteRequest;
import com.careerlink.backend.dto.ConsultationCreateRequest;
import com.careerlink.backend.dto.ConsultationResponse;
import com.careerlink.backend.service.ConsultationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/consultations")
public class ConsultationController {

	private final ConsultationService consultationService;

	public ConsultationController(ConsultationService consultationService) {
		this.consultationService = consultationService;
	}

	@PostMapping
	public ResponseEntity<ConsultationResponse> createConsultation(
		@Valid @RequestBody ConsultationCreateRequest request
	) {
		ConsultationResponse response = ConsultationResponse.from(
			consultationService.createConsultation(
				request.studentSessionId(),
				request.typeId()
			)
		);

		return ResponseEntity
			.created(URI.create("/api/consultations/" + response.id()))
			.body(response);
	}

	@GetMapping("/{id}")
	public ConsultationResponse getConsultation(@PathVariable Long id) {
		Consultation consultation = consultationService.getConsultation(id);
		ConsultationResult result = consultationService.getConsultationResult(id);
		return ConsultationResponse.from(consultation, result);
	}

	@GetMapping
	public List<ConsultationResponse> getConsultationsByStudentPhone(@RequestParam String studentPhone) {
		return consultationService.findByStudentPhone(studentPhone)
			.stream()
			.map(c -> ConsultationResponse.from(c, consultationService.getConsultationResult(c.getId())))
			.toList();
	}

	@PatchMapping("/{id}/accept")
	public ConsultationResponse acceptConsultation(
		@PathVariable Long id,
		@Valid @RequestBody AcceptRequest request
	) {
		return ConsultationResponse.from(consultationService.accept(id, request.counselorName()));
	}

	@PatchMapping("/{id}/cancel-accept")
	public ConsultationResponse cancelAcceptConsultation(@PathVariable Long id) {
		return ConsultationResponse.from(consultationService.cancelAccept(id));
	}

	@PatchMapping("/{id}/start-progress")
	public ConsultationResponse startProgressConsultation(@PathVariable Long id) {
		return ConsultationResponse.from(consultationService.startProgress(id));
	}

	@PatchMapping("/{id}/complete")
	public ConsultationResponse completeConsultation(
		@PathVariable Long id,
		@Valid @RequestBody(required = false) ConsultationCompleteRequest request
	) {
		Consultation consultation = consultationService.complete(id, request);
		ConsultationResult result = consultationService.getConsultationResult(id);
		return ConsultationResponse.from(consultation, result);
	}

	@PatchMapping("/{id}/cancel")
	public ConsultationResponse cancelConsultation(@PathVariable Long id) {
		return ConsultationResponse.from(consultationService.cancel(id));
	}
}
