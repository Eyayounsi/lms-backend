package com.elearning.ProjetPfe.mongo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.elearning.ProjetPfe.mongo.document.AuthLogDocument;

public interface AuthLogRepository extends MongoRepository<AuthLogDocument, String> {

	Page<AuthLogDocument> findByProviderContainingIgnoreCaseAndEmailContainingIgnoreCase(
			String provider,
			String email,
			Pageable pageable
	);
}
