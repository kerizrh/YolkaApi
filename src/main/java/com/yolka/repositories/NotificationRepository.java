package com.yolka.repositories;

import com.yolka.models.NotificationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface NotificationRepository extends MongoRepository<NotificationDocument, String> {
	List<NotificationDocument> findByUserIdAndDeletedFalseOrderByNextNotificationDateAsc(String userId);
	List<NotificationDocument> findByActiveTrueAndDeletedFalseAndNextNotificationDateLessThanEqual(Instant now);
	void deleteByUserId(String userId);
}
