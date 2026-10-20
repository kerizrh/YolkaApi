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
@Document(collection = "process_tokens")
@CompoundIndexes({
		@CompoundIndex(name = "idx_tokens_token_type_deleted", def = "{'token': 1, 'type': 1, 'deleted': 1}"),
		@CompoundIndex(name = "idx_tokens_user_deleted", def = "{'userId': 1, 'deleted': 1}")
})
public class ProcessTokenDocument {

	@Id
	@Builder.Default
	private String id = UuidGeneratorHelper.newUuid();

	@Indexed
	private String token;

	private String type; // accessToken | refreshToken | passwordReset

	@Indexed
	private String userId;

	private String deviceId;

	@Indexed(expireAfter = "0s")
	private Instant expires;

	@Builder.Default
	private boolean deleted = false;

	private Instant deletedAt;
}
