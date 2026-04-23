package com.elearning.ProjetPfe.mongo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.elearning.ProjetPfe.mongo.document.RecommendationLogDocument;

public interface RecommendationLogRepository extends MongoRepository<RecommendationLogDocument, String> {

	Page<RecommendationLogDocument> findByRecommendationTypeContainingIgnoreCase(
			String recommendationType,
			Pageable pageable
	);
}
