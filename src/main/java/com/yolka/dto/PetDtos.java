package com.yolka.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.List;

public class PetDtos {

	public record CreatePetRequest(
			String userId,
			@NotBlank(message = "El nombre de la mascota es obligatorio") String name,
			@NotBlank(message = "La especie es obligatoria (DOG o CAT)") String species,
			String breed,
			String color,
			String gender,
			String birthDate,
			Integer ageYears,
			@Positive(message = "El peso debe ser mayor a 0") Double weight,
			String size,
			String photoUrl
	) {}

	public record UpdatePetRequest(
			String name,
			String species,
			String breed,
			String color,
			String gender,
			String birthDate,
			Integer ageYears,
			Double weight,
			String size,
			String photoUrl
	) {}

	public record VaccineRequest(
			@NotBlank(message = "El nombre de la vacuna es obligatorio") String vaccineName,
			@NotBlank(message = "La fecha de aplicación es obligatoria") String applicationDate,
			String nextDueDate,
			String notes
	) {}

	public record DewormingRequest(
			@NotBlank(message = "El tipo de desparasitación es obligatorio (INTERNAL o EXTERNAL)") String type,
			@NotBlank(message = "El producto es obligatorio") String productName,
			@NotBlank(message = "La fecha de aplicación es obligatoria") String applicationDate,
			String nextDueDate,
			String notes
	) {}

	public record AppointmentRequest(
			@NotBlank(message = "El nombre de la veterinaria o clínica es obligatorio") String clinicName,
			String veterinarianName,
			@NotBlank(message = "El motivo de la cita es obligatorio") String reason,
			@NotBlank(message = "La fecha y hora de la cita son obligatorias") String dateTime,
			String notes,
			Boolean completed
	) {}

	public record RoutineRequest(
			@NotBlank(message = "La categoría de rutina es obligatoria (FEEDING, WALK, GROOMING_BATH, GROOMING_NAILS)") String category,
			@NotBlank(message = "El título de la rutina es obligatorio") String title,
			@NotBlank(message = "La hora (HH:mm) es obligatoria") String time,
			List<String> daysOfWeek,
			Boolean enabled,
			String notes
	) {}

	public record ReminderRequest(
			@NotBlank(message = "El tipo de recordatorio es obligatorio") String type,
			@NotBlank(message = "El título del recordatorio es obligatorio") String title,
			@NotBlank(message = "La fecha y hora son obligatorias") String dateTime,
			Boolean completed
	) {}
}
