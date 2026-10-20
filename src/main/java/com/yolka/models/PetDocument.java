package com.yolka.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
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
public class PetDocument {

	@Id
	private String id;

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

	@Builder.Default
	private Instant createdAt = Instant.now();

	@Builder.Default
	private Instant updatedAt = Instant.now();

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class VaccineRecord {
		private String id;
		private String vaccineName;
		private String applicationDate;
		private String nextDueDate;
		private String notes;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class DewormingRecord {
		private String id;
		private String type; // INTERNAL | EXTERNAL
		private String productName;
		private String applicationDate;
		private String nextDueDate;
		private String notes;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class AppointmentRecord {
		private String id;
		private String clinicName;
		private String veterinarianName;
		private String reason;
		private String dateTime; // ISO-8601
		private String notes;
		private boolean completed;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class RoutineRecord {
		private String id;
		private String category; // FEEDING | WALK | GROOMING_BATH | GROOMING_NAILS
		private String title;
		private String time; // HH:mm
		@Builder.Default
		private List<String> daysOfWeek = new ArrayList<>(); // MON, TUE, WED, THU, FRI, SAT, SUN
		@Builder.Default
		private boolean enabled = true;
		private String notes;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ReminderRecord {
		private String id;
		private String type; // VACCINE | DEWORMING | APPOINTMENT | FEEDING | WALK | GROOMING
		private String title;
		private String dateTime; // ISO-8601
		private boolean completed;
	}
}
