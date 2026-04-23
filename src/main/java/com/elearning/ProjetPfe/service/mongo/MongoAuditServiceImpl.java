package com.elearning.ProjetPfe.service.mongo;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.elearning.ProjetPfe.mongo.document.AiSessionLogDocument;
import com.elearning.ProjetPfe.mongo.document.AuthLogDocument;
import com.elearning.ProjetPfe.mongo.document.ErrorLogDocument;
import com.elearning.ProjetPfe.mongo.document.RecommendationLogDocument;
import com.elearning.ProjetPfe.mongo.repository.AiSessionLogRepository;
import com.elearning.ProjetPfe.mongo.repository.AuthLogRepository;
import com.elearning.ProjetPfe.mongo.repository.ErrorLogRepository;
import com.elearning.ProjetPfe.mongo.repository.RecommendationLogRepository;

@Service
@ConditionalOnProperty(prefix = "app.mongo", name = "enabled", havingValue = "true")
public class MongoAuditServiceImpl implements MongoAuditService {

    private final AuthLogRepository authLogRepository;
    private final RecommendationLogRepository recommendationLogRepository;
    private final AiSessionLogRepository aiSessionLogRepository;
    private final ErrorLogRepository errorLogRepository;

    public MongoAuditServiceImpl(AuthLogRepository authLogRepository,
                                 RecommendationLogRepository recommendationLogRepository,
                                 AiSessionLogRepository aiSessionLogRepository,
                                 ErrorLogRepository errorLogRepository) {
        this.authLogRepository = authLogRepository;
        this.recommendationLogRepository = recommendationLogRepository;
        this.aiSessionLogRepository = aiSessionLogRepository;
        this.errorLogRepository = errorLogRepository;
    }

    @Override
    public void logAuthEvent(String provider, String email, boolean success, String role, String reason) {
        try {
            AuthLogDocument doc = new AuthLogDocument();
            doc.setProvider(provider);
            doc.setEmail(email);
            doc.setSuccess(success);
            doc.setRole(role);
            doc.setReason(reason);
            authLogRepository.save(doc);
        } catch (Exception ex) {
            logErrorEvent("spring-boot", "AUTH_EVENT", ex.getMessage(), getStackTrace(ex), "provider=" + provider + ", email=" + email);
        }
    }

    @Override
    public void logRecommendationEvent(Long courseId,
                                       String recommendationType,
                                       String recommendationBadge,
                                       Double engagementScore,
                                       Integer totalDetections,
                                       Long enrollmentCount,
                                       Double avgRating) {
        try {
            RecommendationLogDocument doc = new RecommendationLogDocument();
            doc.setCourseId(courseId);
            doc.setRecommendationType(recommendationType);
            doc.setRecommendationBadge(recommendationBadge);
            doc.setEngagementScore(engagementScore);
            doc.setTotalDetections(totalDetections);
            doc.setEnrollmentCount(enrollmentCount);
            doc.setAvgRating(avgRating);
            recommendationLogRepository.save(doc);
        } catch (Exception ex) {
            logErrorEvent("spring-boot", "RECOMMENDATION_EVENT", ex.getMessage(), getStackTrace(ex), "courseId=" + courseId);
        }
    }

    @Override
    public void logAiSession(String sessionId,
                             String userEmail,
                             String model,
                             String prompt,
                             String response,
                             Integer latencyMs,
                             Boolean success) {
        try {
            AiSessionLogDocument doc = new AiSessionLogDocument();
            doc.setSessionId(sessionId);
            doc.setUserEmail(userEmail);
            doc.setModel(model);
            doc.setPrompt(prompt);
            doc.setResponse(response);
            doc.setLatencyMs(latencyMs);
            doc.setSuccess(success);
            aiSessionLogRepository.save(doc);
        } catch (Exception ex) {
            logErrorEvent("spring-boot", "AI_SESSION", ex.getMessage(), getStackTrace(ex), "sessionId=" + sessionId);
        }
    }
    @Override
    public void logErrorEvent(String service, String event, String message, String stacktrace, String details) {
        try {
            ErrorLogDocument doc = new ErrorLogDocument();
            doc.setService(service);
            doc.setEvent(event);
            doc.setMessage(message);
            doc.setStacktrace(stacktrace);
            doc.setDetails(details);
            errorLogRepository.save(doc);
        } catch (Exception ignored) {
        }
    }

    private String getStackTrace(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement elem : ex.getStackTrace()) {
            sb.append(elem.toString()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public Page<AuthLogDocument> getAuthLogs(int page, int size, String provider, String email) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return authLogRepository.findByProviderContainingIgnoreCaseAndEmailContainingIgnoreCase(
                provider == null ? "" : provider,
                email == null ? "" : email,
                pageable
        );
    }

    @Override
    public Page<RecommendationLogDocument> getRecommendationLogs(int page, int size, String recommendationType) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return recommendationLogRepository.findByRecommendationTypeContainingIgnoreCase(
                recommendationType == null ? "" : recommendationType,
                pageable
        );
    }

    @Override
    public Page<AiSessionLogDocument> getAiSessionLogs(int page, int size, String sessionId, String userEmail) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return aiSessionLogRepository.findBySessionIdContainingIgnoreCaseAndUserEmailContainingIgnoreCase(
                sessionId == null ? "" : sessionId,
                userEmail == null ? "" : userEmail,
                pageable
        );
    }
}
