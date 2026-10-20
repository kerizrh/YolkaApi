package com.yolka.repositories;

import com.yolka.models.UserDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<UserDocument, String> {
	Optional<UserDocument> findByEmailIgnoreCase(String email);
	boolean existsByEmailIgnoreCase(String email);
}
