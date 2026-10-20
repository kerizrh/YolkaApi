package com.yolka.repositories;

import com.yolka.models.VetClinicDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VetClinicRepository extends MongoRepository<VetClinicDocument, String> {
	List<VetClinicDocument> findByDeletedFalse();
}
