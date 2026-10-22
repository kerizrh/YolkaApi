package com.yolka.services;

import com.yolka.dto.PetDtos.*;
import com.yolka.dto.UserDtos.CreateNotificationRequest;
import com.yolka.helpers.CustomError;
import com.yolka.helpers.UuidGeneratorHelper;
import com.yolka.models.NotificationDocument;
import com.yolka.models.PetDocument;
import com.yolka.models.PetDocument.*;
import com.yolka.repositories.NotificationRepository;
import com.yolka.repositories.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PetsService {

	private final PetRepository petRepository;
	private final NotificationRepository notificationRepository;
	private final NotificationsService notificationsService;
	private final UuidGeneratorHelper uuidGeneratorHelper;

	public List<PetDocument> getPetsByUser(String userId) {
		return petRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId);
	}

	public PetDocument getPetById(String id) {
		return petRepository.findByIdAndDeletedFalse(id)
				.orElseThrow(() -> CustomError.notFound("Mascota no encontrada", "PET_NOT_FOUND"));
	}

	public PetDocument createPet(CreatePetRequest request, String authenticatedUserId) {
		String species = request.species().trim().toUpperCase();
		if (!"DOG".equals(species) && !"CAT".equals(species)) {
			throw CustomError.badRequest("Yolka soporta inicialmente perros (DOG) y gatos (CAT)", "UNSUPPORTED_SPECIES");
		}

		String ownerId = (request.userId() != null && !request.userId().isBlank())
				? request.userId()
				: authenticatedUserId;
		String petUuid = uuidGeneratorHelper.generate();

		PetDocument pet = PetDocument.builder()
				.id(petUuid)
				.uuid(petUuid)
				.userId(ownerId)
				.name(request.name().trim())
				.species(species)
				.breed(request.breed() != null ? request.breed().trim() : "Mestizo")
				.color(request.color() != null ? request.color().trim() : "")
				.gender(request.gender() != null ? request.gender().trim().toUpperCase() : "MALE")
				.birthDate(request.birthDate() != null ? request.birthDate().trim() : "")
				.ageYears(request.ageYears())
				.weight(request.weight() != null ? request.weight() : 0.0)
				.size(request.size() != null ? request.size().trim().toUpperCase() : "MEDIUM")
				.photoUrl(request.photoUrl() != null ? request.photoUrl().trim() : "")
				.deleted(false)
				.createdAt(Instant.now())
				.updatedAt(Instant.now())
				.build();

		return petRepository.save(pet);
	}

	public PetDocument updatePet(String id, UpdatePetRequest request) {
		PetDocument pet = getPetById(id);
		if (request.name() != null && !request.name().isBlank()) pet.setName(request.name().trim());
		if (request.species() != null && !request.species().isBlank()) {
			String species = request.species().trim().toUpperCase();
			if (!"DOG".equals(species) && !"CAT".equals(species)) {
				throw CustomError.badRequest("Yolka soporta inicialmente perros (DOG) y gatos (CAT)", "UNSUPPORTED_SPECIES");
			}
			pet.setSpecies(species);
		}
		if (request.breed() != null) pet.setBreed(request.breed().trim());
		if (request.color() != null) pet.setColor(request.color().trim());
		if (request.gender() != null) pet.setGender(request.gender().trim().toUpperCase());
		if (request.birthDate() != null) pet.setBirthDate(request.birthDate().trim());
		if (request.ageYears() != null) pet.setAgeYears(request.ageYears());
		if (request.weight() != null) pet.setWeight(request.weight());
		if (request.size() != null) pet.setSize(request.size().trim().toUpperCase());
		if (request.photoUrl() != null) pet.setPhotoUrl(request.photoUrl().trim());

		pet.setUpdatedAt(Instant.now());
		return petRepository.save(pet);
	}

	public void deletePet(String id) {
		PetDocument pet = getPetById(id);
		Instant now = Instant.now();
		pet.setDeleted(true);
		pet.setDeletedAt(now);
		pet.setUpdatedAt(now);
		petRepository.save(pet);

		if (pet.getUserId() != null) {
			List<NotificationDocument> notifs = notificationRepository
					.findByUserIdAndDeletedFalseOrderByNextNotificationDateAsc(pet.getUserId());
			notifs.stream()
					.filter(n -> id.equals(n.getPetId()))
					.forEach(n -> {
						n.setActive(false);
						n.setDeleted(true);
						n.setDeletedAt(now);
						notificationRepository.save(n);
					});
		}
	}

	public PetDocument addVaccine(String petId, VaccineRequest request) {
		PetDocument pet = getPetById(petId);
		VaccineRecord record = VaccineRecord.builder()
				.id(uuidGeneratorHelper.generate())
				.vaccineName(request.vaccineName().trim())
				.applicationDate(request.applicationDate().trim())
				.nextDueDate(request.nextDueDate() != null ? request.nextDueDate().trim() : "")
				.notes(request.notes() != null ? request.notes().trim() : "")
				.deleted(false)
				.build();
		pet.getVaccines().add(record);
		pet.setUpdatedAt(Instant.now());
		pet = petRepository.save(pet);

		if (record.getNextDueDate() != null && !record.getNextDueDate().isBlank()) {
			notificationsService.createNotification(new CreateNotificationRequest(
					pet.getUserId(),
					pet.getId(),
					pet.getName(),
					"VACCINE",
					"Vacuna próxima para " + pet.getName(),
					"Recordatorio de vacuna: " + record.getVaccineName(),
					record.getNextDueDate(),
					"America/Mexico_City",
					"once",
					1,
					List.of()
			));
		}
		return pet;
	}

	public PetDocument addDeworming(String petId, DewormingRequest request) {
		PetDocument pet = getPetById(petId);
		DewormingRecord record = DewormingRecord.builder()
				.id(uuidGeneratorHelper.generate())
				.type(request.type().trim().toUpperCase())
				.productName(request.productName().trim())
				.applicationDate(request.applicationDate().trim())
				.nextDueDate(request.nextDueDate() != null ? request.nextDueDate().trim() : "")
				.notes(request.notes() != null ? request.notes().trim() : "")
				.deleted(false)
				.build();
		pet.getDewormings().add(record);
		pet.setUpdatedAt(Instant.now());
		pet = petRepository.save(pet);

		if (record.getNextDueDate() != null && !record.getNextDueDate().isBlank()) {
			notificationsService.createNotification(new CreateNotificationRequest(
					pet.getUserId(),
					pet.getId(),
					pet.getName(),
					"DEWORMING",
					"Desparasitación de " + pet.getName(),
					"Toca desparasitación (" + record.getType() + "): " + record.getProductName(),
					record.getNextDueDate(),
					"America/Mexico_City",
					"once",
					1,
					List.of()
			));
		}
		return pet;
	}

	public PetDocument addAppointment(String petId, AppointmentRequest request) {
		PetDocument pet = getPetById(petId);
		AppointmentRecord record = AppointmentRecord.builder()
				.id(uuidGeneratorHelper.generate())
				.clinicName(request.clinicName().trim())
				.veterinarianName(request.veterinarianName() != null ? request.veterinarianName().trim() : "")
				.reason(request.reason().trim())
				.dateTime(request.dateTime().trim())
				.notes(request.notes() != null ? request.notes().trim() : "")
				.completed(Boolean.TRUE.equals(request.completed()))
				.deleted(false)
				.build();
		pet.getAppointments().add(record);
		pet.setUpdatedAt(Instant.now());
		pet = petRepository.save(pet);

		notificationsService.createNotification(new CreateNotificationRequest(
				pet.getUserId(),
				pet.getId(),
				pet.getName(),
				"APPOINTMENT",
				"Cita médica para " + pet.getName(),
				record.getReason() + " en " + record.getClinicName(),
				record.getDateTime(),
				"America/Mexico_City",
				"once",
				1,
				List.of()
		));
		return pet;
	}

	public PetDocument addRoutine(String petId, RoutineRequest request) {
		PetDocument pet = getPetById(petId);
		RoutineRecord record = RoutineRecord.builder()
				.id(uuidGeneratorHelper.generate())
				.category(request.category().trim().toUpperCase())
				.title(request.title().trim())
				.time(request.time().trim())
				.daysOfWeek(request.daysOfWeek() != null ? request.daysOfWeek() : List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"))
				.enabled(request.enabled() == null || request.enabled())
				.notes(request.notes() != null ? request.notes().trim() : "")
				.deleted(false)
				.build();
		pet.getRoutines().add(record);
		pet.setUpdatedAt(Instant.now());
		pet = petRepository.save(pet);

		if (record.isEnabled()) {
			notificationsService.createNotification(new CreateNotificationRequest(
					pet.getUserId(),
					pet.getId(),
					pet.getName(),
					record.getCategory(),
					record.getTitle() + " — " + pet.getName(),
					"Es hora de: " + record.getTitle() + " para " + pet.getName(),
					record.getTime(),
					"America/Mexico_City",
					"days",
					1,
					record.getDaysOfWeek()
			));
		}
		return pet;
	}

	public PetDocument addReminder(String petId, ReminderRequest request) {
		PetDocument pet = getPetById(petId);
		ReminderRecord record = ReminderRecord.builder()
				.id(uuidGeneratorHelper.generate())
				.type(request.type().trim().toUpperCase())
				.title(request.title().trim())
				.dateTime(request.dateTime().trim())
				.completed(Boolean.TRUE.equals(request.completed()))
				.deleted(false)
				.build();
		pet.getReminders().add(record);
		pet.setUpdatedAt(Instant.now());
		return petRepository.save(pet);
	}

	public PetDocument toggleReminder(String petId, String reminderId, boolean completed) {
		PetDocument pet = getPetById(petId);
		pet.getReminders().stream()
				.filter(r -> !r.isDeleted() && reminderId.equals(r.getId()))
				.findFirst()
				.ifPresent(r -> r.setCompleted(completed));
		pet.setUpdatedAt(Instant.now());
		return petRepository.save(pet);
	}

	public PetDocument updateVaccine(String petId, String vaccineId, VaccineRequest request) {
		PetDocument pet = getPetById(petId);
		if (pet.getVaccines() != null) {
			for (VaccineRecord rec : pet.getVaccines()) {
				if (!rec.isDeleted() && vaccineId.equals(rec.getId())) {
					rec.setVaccineName(request.vaccineName().trim());
					rec.setApplicationDate(request.applicationDate().trim());
					rec.setNextDueDate(request.nextDueDate() != null ? request.nextDueDate().trim() : "");
					rec.setNotes(request.notes() != null ? request.notes().trim() : "");
					break;
				}
			}
		}
		pet.setUpdatedAt(Instant.now());
		return petRepository.save(pet);
	}

	public PetDocument updateDeworming(String petId, String dewormingId, DewormingRequest request) {
		PetDocument pet = getPetById(petId);
		if (pet.getDewormings() != null) {
			for (DewormingRecord rec : pet.getDewormings()) {
				if (!rec.isDeleted() && dewormingId.equals(rec.getId())) {
					rec.setType(request.type().trim().toUpperCase());
					rec.setProductName(request.productName().trim());
					rec.setApplicationDate(request.applicationDate().trim());
					rec.setNextDueDate(request.nextDueDate() != null ? request.nextDueDate().trim() : "");
					rec.setNotes(request.notes() != null ? request.notes().trim() : "");
					break;
				}
			}
		}
		pet.setUpdatedAt(Instant.now());
		return petRepository.save(pet);
	}

	public PetDocument updateAppointment(String petId, String appointmentId, AppointmentRequest request) {
		PetDocument pet = getPetById(petId);
		if (pet.getAppointments() != null) {
			for (AppointmentRecord rec : pet.getAppointments()) {
				if (!rec.isDeleted() && appointmentId.equals(rec.getId())) {
					rec.setClinicName(request.clinicName().trim());
					rec.setVeterinarianName(request.veterinarianName() != null ? request.veterinarianName().trim() : "");
					rec.setReason(request.reason().trim());
					rec.setDateTime(request.dateTime().trim());
					rec.setNotes(request.notes() != null ? request.notes().trim() : "");
					if (request.completed() != null) {
						rec.setCompleted(request.completed());
					}
					break;
				}
			}
		}
		pet.setUpdatedAt(Instant.now());
		return petRepository.save(pet);
	}
}
