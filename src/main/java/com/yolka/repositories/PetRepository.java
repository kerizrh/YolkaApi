package com.yolka.repositories;

import com.yolka.models.PetDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PetRepository extends MongoRepository<PetDocument, String> {
	List<PetDocument> findByUserIdOrderByCreatedAtDesc(String userId);
	void deleteByUserId(String userId);
}
