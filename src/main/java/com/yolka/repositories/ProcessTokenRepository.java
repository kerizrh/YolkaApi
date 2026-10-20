package com.yolka.repositories;

import com.yolka.models.ProcessTokenDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessTokenRepository extends MongoRepository<ProcessTokenDocument, String> {
	Optional<ProcessTokenDocument> findByTokenAndType(String token, String type);
	void deleteByUserId(String userId);
}
