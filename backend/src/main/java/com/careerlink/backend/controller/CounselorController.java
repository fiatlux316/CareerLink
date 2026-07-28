package com.careerlink.backend.controller;

import java.net.URI;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

import com.careerlink.backend.domain.ConsultationStatus;
import com.careerlink.backend.dto.ConsultationResponse;
import com.careerlink.backend.dto.CounselorEnterRequest;
import com.careerlink.backend.dto.CounselorSessionResponse;
import com.careerlink.backend.service.ConsultationService;
import com.careerlink.backend.service.CounselorService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/counselor")
public class CounselorController {

	private final CounselorService counselorService;
	private final ConsultationService consultationService;

	public CounselorController(CounselorService counselorService, ConsultationService consultationService) {
		this.counselorService = counselorService;
		this.consultationService = consultationService;
	}

	@PostMapping("/enter")
	public ResponseEntity<CounselorSessionResponse> enter(@Valid @RequestBody CounselorEnterRequest request) {
		CounselorSessionResponse response = CounselorSessionResponse.from(
			counselorService.enter(request.counselorName(), request.counselorPhone(), request.typeId())
		);

		return ResponseEntity
			.created(URI.create("/api/counselor/enter/" + response.id()))
			.body(response);
	}

	@GetMapping("/consultations")
	public List<ConsultationResponse> getConsultations(
		@RequestParam Long typeId,
		@RequestParam(defaultValue = "RECEIVED") ConsultationStatus status
	) {
		return consultationService.findByTypeAndStatus(typeId, status)
			.stream()
			.map(ConsultationResponse::from)
			.toList();
	}
}
