package com.yolka.repositories;

import com.yolka.models.ProcessTokenDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcessTokenRepository extends MongoRepository<ProcessTokenDocument, String> {
	Optional<ProcessTokenDocument> findByTokenAndTypeAndDeletedFalse(String token, String type);
	List<ProcessTokenDocument> findByUserIdAndDeletedFalse(String userId);
	List<ProcessTokenDocument> findByUserIdAndDeviceIdAndDeletedFalse(String userId, String deviceId);
}
