package com.yolka.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "backups")
public class BackupDocument {

	@Id
	private String id;

	@Indexed
	private String userId;

	private String fileName;
	private long fileSize;
	private String type;
	private Instant createdAt;
	private Instant updatedAt;
}
