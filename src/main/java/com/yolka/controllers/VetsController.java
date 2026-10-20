package com.yolka.controllers;

import com.yolka.models.VetClinicDocument;
import com.yolka.repositories.VetClinicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/vets")
@RequiredArgsConstructor
public class VetsController {

	private final VetClinicRepository vetClinicRepository;

	@GetMapping
	public ResponseEntity<Map<String, Object>> getVetClinics() {
		List<VetClinicDocument> clinics = vetClinicRepository.findByDeletedFalse();
		return ResponseEntity.ok(Map.of("success", true, "vets", clinics));
	}
}
