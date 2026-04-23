package com.elearning.ProjetPfe.mongo.document;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "recommendation_logs")
public class RecommendationLogDocument {

    @Id
    private String id;

    @Indexed
    private LocalDateTime createdAt = LocalDateTime.now();

    @Indexed
    private Long courseId;

    @Indexed
    private String recommendationType;

    private String recommendationBadge;
    private Double engagementScore;
    private Integer totalDetections;
    private Long enrollmentCount;
    private Double avgRating;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getRecommendationType() { return recommendationType; }
    public void setRecommendationType(String recommendationType) { this.recommendationType = recommendationType; }

    public String getRecommendationBadge() { return recommendationBadge; }
    public void setRecommendationBadge(String recommendationBadge) { this.recommendationBadge = recommendationBadge; }

    public Double getEngagementScore() { return engagementScore; }
    public void setEngagementScore(Double engagementScore) { this.engagementScore = engagementScore; }

    public Integer getTotalDetections() { return totalDetections; }
    public void setTotalDetections(Integer totalDetections) { this.totalDetections = totalDetections; }

    public Long getEnrollmentCount() { return enrollmentCount; }
    public void setEnrollmentCount(Long enrollmentCount) { this.enrollmentCount = enrollmentCount; }

    public Double getAvgRating() { return avgRating; }
    public void setAvgRating(Double avgRating) { this.avgRating = avgRating; }
}
