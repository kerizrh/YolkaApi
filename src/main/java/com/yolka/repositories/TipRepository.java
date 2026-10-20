package com.yolka.repositories;

import com.yolka.models.TipDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipRepository extends MongoRepository<TipDocument, String> {
	List<TipDocument> findByDeletedFalse();
	List<TipDocument> findByCategoryIgnoreCaseAndDeletedFalse(String category);
	List<TipDocument> findBySpeciesIgnoreCaseInAndDeletedFalse(List<String> species);
}
