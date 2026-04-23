package com.elearning.ProjetPfe.mongo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.elearning.ProjetPfe.mongo.document.AiSessionLogDocument;

public interface AiSessionLogRepository extends MongoRepository<AiSessionLogDocument, String> {

	Page<AiSessionLogDocument> findBySessionIdContainingIgnoreCaseAndUserEmailContainingIgnoreCase(
			String sessionId,
			String userEmail,
			Pageable pageable
	);
}
