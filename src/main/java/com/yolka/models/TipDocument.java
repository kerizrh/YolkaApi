package com.yolka.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "tips")
public class TipDocument {

	@Id
	private String id;

	private String category; // FEEDING | HYGIENE | EXERCISE | VACCINATION | DEWORMING | GENERAL_CARE | WELLNESS | FUN_FACTS
	private String species;  // DOG | CAT | ALL
	private String title;
	private String summary;
	private String content;
	private String imageUrl;
}
