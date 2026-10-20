package com.yolka.repositories;

import com.yolka.models.PetDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PetRepository extends MongoRepository<PetDocument, String> {
	Optional<PetDocument> findByIdAndDeletedFalse(String id);
	List<PetDocument> findByUserIdAndDeletedFalseOrderByCreatedAtDesc(String userId);
}
