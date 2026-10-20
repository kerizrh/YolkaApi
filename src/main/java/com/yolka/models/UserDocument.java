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
@Document(collection = "users")
@CompoundIndexes({
		@CompoundIndex(name = "idx_users_email_deleted", def = "{'email': 1, 'deleted': 1}"),
		@CompoundIndex(name = "idx_users_google_deleted", def = "{'googleId': 1, 'deleted': 1}")
})
public class UserDocument {

	@Id
	@Builder.Default
	private String id = UuidGeneratorHelper.newUuid();

	@Indexed(unique = true)
	@Builder.Default
	private String uuid = UuidGeneratorHelper.newUuid();

	private String fullName;
	private String firstName;
	private String lastName;

	@Indexed
	private String email;

	private String password;

	@Builder.Default
	private String authProvider = "LOCAL"; // LOCAL | GOOGLE

	@Indexed(sparse = true)
	private String googleId;

	private String phone;
	private String photoUrl;
	private String fcmToken;

	@Builder.Default
	private List<DeviceInfo> devices = new ArrayList<>();

	@Builder.Default
	private List<DeviceInfo> activeDevices = new ArrayList<>();

	@Builder.Default
	private UserPreferences preferences = new UserPreferences();

	@Builder.Default
	private boolean hasDeviceBackup = false;

	private Instant lastDeviceBackupAt;

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
	public static class DeviceInfo {
		private String deviceId;
		private String fcmToken;
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class UserPreferences {
		@Builder.Default
		private boolean darkMode = false;

		@Builder.Default
		private String language = "es";

		@Builder.Default
		private String timeZone = "America/Mexico_City";

		@Builder.Default
		private NotificationPreferences notifications = new NotificationPreferences();
	}

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class NotificationPreferences {
		@Builder.Default
		private boolean vaccines = true;

		@Builder.Default
		private boolean deworming = true;

		@Builder.Default
		private boolean medicalAppointments = true;

		@Builder.Default
		private boolean grooming = true;

		@Builder.Default
		private boolean feeding = true;

		@Builder.Default
		private boolean walks = true;
	}
}
