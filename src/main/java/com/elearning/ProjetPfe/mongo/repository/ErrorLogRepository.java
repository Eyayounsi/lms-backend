package com.elearning.ProjetPfe.mongo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.elearning.ProjetPfe.mongo.document.ErrorLogDocument;

public interface ErrorLogRepository extends MongoRepository<ErrorLogDocument, String> {
}
