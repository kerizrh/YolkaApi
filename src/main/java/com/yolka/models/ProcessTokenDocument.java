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
@Document(collection = "process_tokens")
public class ProcessTokenDocument {

	@Id
	private String id;

	@Indexed
	private String token;

	private String type; // accessToken | refreshToken | passwordReset
	private String userId;
	private String deviceId;
	private Instant expires;
}
