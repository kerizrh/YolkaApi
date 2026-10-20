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
@Document(collection = "notifications")
public class NotificationDocument {

	@Id
	private String id;

	@Indexed
	private String userId;

	private String petId;
	private String petName;
	private String category; // VACCINE | DEWORMING | APPOINTMENT | GROOMING | FEEDING | WALK
	private String title;
	private String body;
	private String scheduledTime; // ISO-8601 local or HH:mm
	private Instant nextNotificationDate;
	private String timeZone;
	private String frequencyUnit; // once | days | weeks | months | years
	private int frequencyInterval;

	@Builder.Default
	private List<String> daysOfWeek = new ArrayList<>();

	@Builder.Default
	private boolean active = true;

	@Builder.Default
	private boolean deleted = false;

	@Builder.Default
	private Instant createdAt = Instant.now();
}
