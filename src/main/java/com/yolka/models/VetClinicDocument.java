package com.yolka.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "vet_clinics")
public class VetClinicDocument {

	@Id
	private String id;

	private String name;
	private String address;
	private String phone;
	private double latitude;
	private double longitude;
	private String openingHours;

	@Builder.Default
	private List<String> services = new ArrayList<>();
}
