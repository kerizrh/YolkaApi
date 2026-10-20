package com.yolka.controllers;

import com.yolka.models.TipDocument;
import com.yolka.repositories.TipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/tips", "/api/tips"})
@RequiredArgsConstructor
public class TipsController {

	private final TipRepository tipRepository;

	@GetMapping
	public ResponseEntity<Map<String, Object>> getTips(
			@RequestParam(required = false) String category,
			@RequestParam(required = false) String species
	) {
		List<TipDocument> tips;
		if (category != null && !category.isBlank()) {
			tips = tipRepository.findByCategoryIgnoreCaseAndDeletedFalse(category.trim());
		} else if (species != null && !species.isBlank()) {
			tips = tipRepository.findBySpeciesIgnoreCaseInAndDeletedFalse(List.of(species.trim().toUpperCase(), "ALL"));
		} else {
			tips = tipRepository.findByDeletedFalse();
		}
		return ResponseEntity.ok(Map.of("success", true, "tips", tips));
	}
}
