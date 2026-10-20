package com.yolka.models;

import com.yolka.helpers.UuidGeneratorHelper;
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
@Document(collection = "vet_clinics")
public class VetClinicDocument {

	@Id
	@Builder.Default
	private String id = UuidGeneratorHelper.newUuid();

	@Indexed(unique = true)
	@Builder.Default
	private String uuid = UuidGeneratorHelper.newUuid();

	@Indexed
	private String name;

	private String address;
	private String phone;
	private double latitude;
	private double longitude;
	private String openingHours;

	@Builder.Default
	private List<String> services = new ArrayList<>();

	@Indexed
	@Builder.Default
	private boolean deleted = false;

	private Instant deletedAt;
}
