package com.elearning.ProjetPfe.service.mongo;

import org.springframework.data.domain.Page;

import com.elearning.ProjetPfe.mongo.document.AiSessionLogDocument;
import com.elearning.ProjetPfe.mongo.document.AuthLogDocument;
import com.elearning.ProjetPfe.mongo.document.RecommendationLogDocument;

public interface MongoAuditService {

    void logErrorEvent(String service, String event, String message, String stacktrace, String details);
    void logAuthEvent(String provider, String email, boolean success, String role, String reason);

    void logRecommendationEvent(Long courseId,
                                String recommendationType,
                                String recommendationBadge,
                                Double engagementScore,
                                Integer totalDetections,
                                Long enrollmentCount,
                                Double avgRating);

    void logAiSession(String sessionId,
                      String userEmail,
                      String model,
                      String prompt,
                      String response,
                      Integer latencyMs,
                      Boolean success);

    Page<AuthLogDocument> getAuthLogs(int page, int size, String provider, String email);

    Page<RecommendationLogDocument> getRecommendationLogs(int page, int size, String recommendationType);

    Page<AiSessionLogDocument> getAiSessionLogs(int page, int size, String sessionId, String userEmail);
}
