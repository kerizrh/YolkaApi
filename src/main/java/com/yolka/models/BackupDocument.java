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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "backups")
@CompoundIndexes({
		@CompoundIndex(name = "idx_backups_user_deleted_created", def = "{'userId': 1, 'deleted': 1, 'createdAt': -1}")
})
public class BackupDocument {

	@Id
	@Builder.Default
	private String id = UuidGeneratorHelper.newUuid();

	@Indexed(unique = true, sparse = true)
	@Builder.Default
	private String uuid = UuidGeneratorHelper.newUuid();

	@Indexed
	private String userId;

	private String fileName;
	private long fileSize;
	private String type;
	private String driveFileId;
	private String driveWebViewLink;

	@Indexed
	@Builder.Default
	private boolean deleted = false;

	private Instant deletedAt;

	@Builder.Default
	private Instant createdAt = Instant.now();

	@Builder.Default
	private Instant updatedAt = Instant.now();
}
