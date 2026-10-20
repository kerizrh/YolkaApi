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
@Document(collection = "tips")
@CompoundIndexes({
		@CompoundIndex(name = "idx_tips_category_species_deleted", def = "{'category': 1, 'species': 1, 'deleted': 1}")
})
public class TipDocument {

	@Id
	@Builder.Default
	private String id = UuidGeneratorHelper.newUuid();

	@Indexed(unique = true)
	@Builder.Default
	private String uuid = UuidGeneratorHelper.newUuid();

	@Indexed
	private String category; // FEEDING | HYGIENE | EXERCISE | VACCINATION | DEWORMING | GENERAL_CARE | WELLNESS | FUN_FACTS

	@Indexed
	private String species;  // DOG | CAT | ALL

	private String title;
	private String summary;
	private String content;
	private String imageUrl;

	@Indexed
	@Builder.Default
	private boolean deleted = false;

	private Instant deletedAt;
}
