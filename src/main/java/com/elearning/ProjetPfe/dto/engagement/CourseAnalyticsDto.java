package com.elearning.ProjetPfe.dto.engagement;

import java.util.Map;

/**
 * DTO analytics d'un cours — utilisé par les dashboards Instructor & Admin.
 * Agrège : détections caméra + avis + inscriptions → recommandation générée.
 */
public class CourseAnalyticsDto {

    private Long courseId;
    private String title;
    private String coverImage;
    private String instructorName;

    // ── Inscriptions ──────────────────────────────────────────────────────
    private long enrollmentCount;

    // ── Détection caméra ──────────────────────────────────────────────────
    private int totalDetections;
    private double avgAttentionScore;       // 0-100
    private double concentrationRate;      // % CONCENTRATED + SMILING
    private double fatigueRate;            // % EYES_CLOSED + YAWNING + TIRED
    private double distractionRate;        // % LOOKING_AWAY + ABSENT
    private Map<String, Integer> detectionBreakdown; // type → count

    // ── Avis ──────────────────────────────────────────────────────────────
    private Double avgRating;              // null si aucun avis
    private long reviewCount;

    // ── Score global (algorithme pondéré) ─────────────────────────────────
    private double engagementScore;        // 0-100

    // ── Recommandation générée ────────────────────────────────────────────
    private String recommendationType;    // EXCELLENT | SEGMENT_CONTENT | SLOW_DOWN | NEEDS_ENGAGEMENT | ON_TRACK | NO_DATA
    private String recommendationBadge;   // libellé court affiché dans badge
    private String recommendationColor;   // ex. #22c55e
    private String recommendationIcon;    // emoji
    private String recommendationMessage; // texte long (HTML allowed)

    // ─── Constructors ─────────────────────────────────────────────────────

    public CourseAnalyticsDto() {}

    // ─── Getters & Setters ────────────────────────────────────────────────

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

    public long getEnrollmentCount() { return enrollmentCount; }
    public void setEnrollmentCount(long enrollmentCount) { this.enrollmentCount = enrollmentCount; }

    public int getTotalDetections() { return totalDetections; }
    public void setTotalDetections(int totalDetections) { this.totalDetections = totalDetections; }

    public double getAvgAttentionScore() { return avgAttentionScore; }
    public void setAvgAttentionScore(double avgAttentionScore) { this.avgAttentionScore = avgAttentionScore; }

    public double getConcentrationRate() { return concentrationRate; }
    public void setConcentrationRate(double concentrationRate) { this.concentrationRate = concentrationRate; }

    public double getFatigueRate() { return fatigueRate; }
    public void setFatigueRate(double fatigueRate) { this.fatigueRate = fatigueRate; }

    public double getDistractionRate() { return distractionRate; }
    public void setDistractionRate(double distractionRate) { this.distractionRate = distractionRate; }

    public Map<String, Integer> getDetectionBreakdown() { return detectionBreakdown; }
    public void setDetectionBreakdown(Map<String, Integer> detectionBreakdown) { this.detectionBreakdown = detectionBreakdown; }

    public Double getAvgRating() { return avgRating; }
    public void setAvgRating(Double avgRating) { this.avgRating = avgRating; }

    public long getReviewCount() { return reviewCount; }
    public void setReviewCount(long reviewCount) { this.reviewCount = reviewCount; }

    public double getEngagementScore() { return engagementScore; }
    public void setEngagementScore(double engagementScore) { this.engagementScore = engagementScore; }

    public String getRecommendationType() { return recommendationType; }
    public void setRecommendationType(String recommendationType) { this.recommendationType = recommendationType; }

    public String getRecommendationBadge() { return recommendationBadge; }
    public void setRecommendationBadge(String recommendationBadge) { this.recommendationBadge = recommendationBadge; }

    public String getRecommendationColor() { return recommendationColor; }
    public void setRecommendationColor(String recommendationColor) { this.recommendationColor = recommendationColor; }

    public String getRecommendationIcon() { return recommendationIcon; }
    public void setRecommendationIcon(String recommendationIcon) { this.recommendationIcon = recommendationIcon; }

    public String getRecommendationMessage() { return recommendationMessage; }
    public void setRecommendationMessage(String recommendationMessage) { this.recommendationMessage = recommendationMessage; }
}
