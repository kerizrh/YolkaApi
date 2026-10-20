package com.yolka.repositories;

import com.yolka.models.BackupDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BackupRepository extends MongoRepository<BackupDocument, String> {
	Optional<BackupDocument> findFirstByUserIdAndDeletedFalseOrderByCreatedAtDesc(String userId);
	List<BackupDocument> findByUserIdAndDeletedFalse(String userId);
}
