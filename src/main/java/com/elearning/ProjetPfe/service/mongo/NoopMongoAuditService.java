
package com.elearning.ProjetPfe.service.mongo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.elearning.ProjetPfe.mongo.document.AiSessionLogDocument;
import com.elearning.ProjetPfe.mongo.document.AuthLogDocument;
import com.elearning.ProjetPfe.mongo.document.RecommendationLogDocument;

@Service
@ConditionalOnProperty(prefix = "app.mongo", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopMongoAuditService implements MongoAuditService {

    @Override
    public void logErrorEvent(String service, String event, String message, String stacktrace, String details) {
        // No-op
    }

    @Override
    public void logAuthEvent(String provider, String email, boolean success, String role, String reason) {
        // No-op
    }

    @Override
    public void logRecommendationEvent(Long courseId,
                                       String recommendationType,
                                       String recommendationBadge,
                                       Double engagementScore,
                                       Integer totalDetections,
                                       Long enrollmentCount,
                                       Double avgRating) {
        // No-op
    }

    @Override
    public void logAiSession(String sessionId,
                             String userEmail,
                             String model,
                             String prompt,
                             String response,
                             Integer latencyMs,
                             Boolean success) {
        // No-op
    }

    @Override
    public Page<AuthLogDocument> getAuthLogs(int page, int size, String provider, String email) {
        return new PageImpl<>(java.util.List.of(), PageRequest.of(page, size), 0);
    }

    @Override
    public Page<RecommendationLogDocument> getRecommendationLogs(int page, int size, String recommendationType) {
        return new PageImpl<>(java.util.List.of(), PageRequest.of(page, size), 0);
    }

    @Override
    public Page<AiSessionLogDocument> getAiSessionLogs(int page, int size, String sessionId, String userEmail) {
        return new PageImpl<>(java.util.List.of(), PageRequest.of(page, size), 0);
    }
}
