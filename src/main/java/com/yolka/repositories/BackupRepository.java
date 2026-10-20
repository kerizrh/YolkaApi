package com.yolka.repositories;

import com.yolka.models.BackupDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BackupRepository extends MongoRepository<BackupDocument, String> {
	Optional<BackupDocument> findFirstByUserIdOrderByCreatedAtDesc(String userId);
	void deleteByUserId(String userId);
}
