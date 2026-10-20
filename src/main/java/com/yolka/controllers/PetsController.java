package com.yolka.controllers;

import com.yolka.dto.PetDtos.*;
import com.yolka.models.PetDocument;
import com.yolka.services.PetsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
public class PetsController {

	private final PetsService petsService;

	@GetMapping
	public ResponseEntity<Map<String, Object>> getPets(
			@RequestParam(required = false) String userId,
			HttpServletRequest httpRequest
	) {
		String authUserId = (String) httpRequest.getAttribute("authenticatedUserId");
		String targetUserId = (userId != null && !userId.isBlank()) ? userId : authUserId;
		List<PetDocument> pets = targetUserId != null ? petsService.getPetsByUser(targetUserId) : List.of();
		return ResponseEntity.ok(Map.of("success", true, "pets", pets));
	}

	@PostMapping
	public ResponseEntity<Map<String, Object>> createPet(
			@Valid @RequestBody CreatePetRequest request,
			HttpServletRequest httpRequest
	) {
		String authUserId = (String) httpRequest.getAttribute("authenticatedUserId");
		PetDocument pet = petsService.createPet(request, authUserId);
		return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "pet", pet));
	}

	@GetMapping("/{id}")
	public ResponseEntity<Map<String, Object>> getPetById(@PathVariable String id) {
		return ResponseEntity.ok(Map.of("success", true, "pet", petsService.getPetById(id)));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Map<String, Object>> updatePet(
			@PathVariable String id,
			@RequestBody UpdatePetRequest request
	) {
		return ResponseEntity.ok(Map.of("success", true, "pet", petsService.updatePet(id, request)));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Map<String, Object>> deletePet(@PathVariable String id) {
		petsService.deletePet(id);
		return ResponseEntity.ok(Map.of("success", true, "message", "Mascota eliminada correctamente"));
	}

	@PostMapping("/{id}/vaccines")
	public ResponseEntity<Map<String, Object>> addVaccine(
			@PathVariable String id,
			@Valid @RequestBody VaccineRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "pet", petsService.addVaccine(id, request)));
	}

	@PostMapping("/{id}/dewormings")
	public ResponseEntity<Map<String, Object>> addDeworming(
			@PathVariable String id,
			@Valid @RequestBody DewormingRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "pet", petsService.addDeworming(id, request)));
	}

	@PostMapping("/{id}/appointments")
	public ResponseEntity<Map<String, Object>> addAppointment(
			@PathVariable String id,
			@Valid @RequestBody AppointmentRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "pet", petsService.addAppointment(id, request)));
	}

	@PostMapping("/{id}/routines")
	public ResponseEntity<Map<String, Object>> addRoutine(
			@PathVariable String id,
			@Valid @RequestBody RoutineRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "pet", petsService.addRoutine(id, request)));
	}

	@PostMapping("/{id}/reminders")
	public ResponseEntity<Map<String, Object>> addReminder(
			@PathVariable String id,
			@Valid @RequestBody ReminderRequest request
	) {
		return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("success", true, "pet", petsService.addReminder(id, request)));
	}

	@PatchMapping("/{id}/reminders/{reminderId}")
	public ResponseEntity<Map<String, Object>> toggleReminder(
			@PathVariable String id,
			@PathVariable String reminderId,
			@RequestParam(defaultValue = "true") boolean completed
	) {
		return ResponseEntity.ok(Map.of("success", true, "pet", petsService.toggleReminder(id, reminderId, completed)));
	}
}
