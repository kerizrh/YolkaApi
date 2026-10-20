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
@Document(collection = "notifications")
@CompoundIndexes({
		@CompoundIndex(name = "idx_notif_user_deleted_next", def = "{'userId': 1, 'deleted': 1, 'nextNotificationDate': 1}"),
		@CompoundIndex(name = "idx_notif_due_cron", def = "{'active': 1, 'deleted': 1, 'nextNotificationDate': 1}")
})
public class NotificationDocument {

	@Id
	@Builder.Default
	private String id = UuidGeneratorHelper.newUuid();

	@Indexed(unique = true, sparse = true)
	@Builder.Default
	private String uuid = UuidGeneratorHelper.newUuid();

	@Indexed
	private String userId;

	@Indexed
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

	@Indexed
	@Builder.Default
	private boolean deleted = false;

	private Instant deletedAt;

	@Builder.Default
	private Instant createdAt = Instant.now();
}
