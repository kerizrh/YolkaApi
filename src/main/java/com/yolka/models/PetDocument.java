package com.yolka.models;

import com.yolka.helpers.UuidGeneratorHelper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "pets")
@CompoundIndexes({
		@CompoundIndex(name = "idx_pets_user_deleted_created", def = "{'userId': 1, 'deleted': 1, 'createdAt': -1}"),
		@CompoundIndex(name = "idx_pets_user_species_deleted", def = "{'userId': 1, 'species': 1, 'deleted': 1}")
})
public class PetDocument {

	@Id
	@Builder.Default
	private String id = UuidGeneratorHelper.newUuid();

	@Indexed(unique = true, sparse = true)
	@Builder.Default
	private String uuid = UuidGeneratorHelper.newUuid();

	@Indexed
	private String userId;

	private String name;
	private String species; // DOG | CAT
	private String breed;
	private String color;
	private String gender; // MALE | FEMALE
	private String birthDate; // YYYY-MM-DD
	private Integer ageYears;
	private Double weight;
	private String size; // SMALL | MEDIUM | LARGE
	private String photoUrl;

	@Builder.Default
	private List<VaccineRecord> vaccines = new ArrayList<>();

	@Builder.Default
	private List<DewormingRecord> dewormings = new ArrayList<>();

	@Builder.Default
	private List<AppointmentRecord> appointments = new ArrayList<>();

	@Builder.Default
	private List<RoutineRecord> routines = new ArrayList<>();

	@Builder.Default
	private List<ReminderRecord> reminders = new ArrayList<>();

	@Indexed
	@Builder.Default
	private boolean deleted = false;

	private Instant deletedAt;

	@Builder.Default
	private Instant createdAt = Instant.now();

	@Builder.Default
	private Instant updatedAt = Instant.now();

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class VaccineRecord {
		@Builder.Default
		private String id = UuidGeneratorHelper.newUuid();
		private String vaccineName;
		private String applicationDate;
		private String nextDueDate;
		private String notes;
		@Builder.Default
		private boolean deleted = false;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class DewormingRecord {
		@Builder.Default
		private String id = UuidGeneratorHelper.newUuid();
		private String type; // INTERNAL | EXTERNAL
		private String productName;
		private String applicationDate;
		private String nextDueDate;
		private String notes;
		@Builder.Default
		private boolean deleted = false;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class AppointmentRecord {
		@Builder.Default
		private String id = UuidGeneratorHelper.newUuid();
		private String clinicName;
		private String veterinarianName;
		private String reason;
		private String dateTime; // ISO-8601
		private String notes;
		private boolean completed;
		@Builder.Default
		private boolean deleted = false;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class RoutineRecord {
		@Builder.Default
		private String id = UuidGeneratorHelper.newUuid();
		private String category; // FEEDING | WALK | GROOMING_BATH | GROOMING_NAILS
		private String title;
		private String time; // HH:mm
		@Builder.Default
		private List<String> daysOfWeek = new ArrayList<>(); // MON, TUE, WED, THU, FRI, SAT, SUN
		@Builder.Default
		private boolean enabled = true;
		private String notes;
		@Builder.Default
		private boolean deleted = false;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ReminderRecord {
		@Builder.Default
		private String id = UuidGeneratorHelper.newUuid();
		private String type; // VACCINE | DEWORMING | APPOINTMENT | FEEDING | WALK | GROOMING
		private String title;
		private String dateTime; // ISO-8601
		private boolean completed;
		@Builder.Default
		private boolean deleted = false;
	}
}
