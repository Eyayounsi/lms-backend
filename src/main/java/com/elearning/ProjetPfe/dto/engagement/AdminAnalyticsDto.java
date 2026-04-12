package com.elearning.ProjetPfe.dto.engagement;

import com.elearning.ProjetPfe.entity.learning.Note;
import com.elearning.ProjetPfe.entity.course.Course;
import java.util.List;

/**
 * DTO du dashboard analytique Admin — statistiques plateforme + recommandations.
 */
public class AdminAnalyticsDto {

    // ── Statistiques globales plateforme ──────────────────────────────────
    private double platformAvgAttention;
    private double platformConcentrationRate;
    private double platformFatigueRate;
    private double platformDistractionRate;
    private int platformTotalDetections;
    private long platformTotalEnrollments;
    private double platformAvgRating;

    // ── Tous les cours avec analytics ────────────────────────────────────
    private List<CourseAnalyticsDto> allCourses;

    // ── Listes filtrées / triées ──────────────────────────────────────────
    /** Top 5 par score d'engagement (à mettre en avant) */
    private List<CourseAnalyticsDto> topEngagementCourses;

    /** Top 5 par note moyenne */
    private List<CourseAnalyticsDto> topRatedCourses;

    /** Cours recommandés pour un Featured/Top Course */
    private List<CourseAnalyticsDto> recommendedForFeatured;

    /** Cours avec engagement faible (< 40) — à améliorer */
    private List<CourseAnalyticsDto> needsImprovementCourses;

    // ─── Getters & Setters ────────────────────────────────────────────────

    public double getPlatformAvgAttention() { return platformAvgAttention; }
    public void setPlatformAvgAttention(double v) { this.platformAvgAttention = v; }

    public double getPlatformConcentrationRate() { return platformConcentrationRate; }
    public void setPlatformConcentrationRate(double v) { this.platformConcentrationRate = v; }

    public double getPlatformFatigueRate() { return platformFatigueRate; }
    public void setPlatformFatigueRate(double v) { this.platformFatigueRate = v; }

    public double getPlatformDistractionRate() { return platformDistractionRate; }
    public void setPlatformDistractionRate(double v) { this.platformDistractionRate = v; }

    public int getPlatformTotalDetections() { return platformTotalDetections; }
    public void setPlatformTotalDetections(int v) { this.platformTotalDetections = v; }

    public long getPlatformTotalEnrollments() { return platformTotalEnrollments; }
    public void setPlatformTotalEnrollments(long v) { this.platformTotalEnrollments = v; }

    public double getPlatformAvgRating() { return platformAvgRating; }
    public void setPlatformAvgRating(double v) { this.platformAvgRating = v; }

    public List<CourseAnalyticsDto> getAllCourses() { return allCourses; }
    public void setAllCourses(List<CourseAnalyticsDto> v) { this.allCourses = v; }

    public List<CourseAnalyticsDto> getTopEngagementCourses() { return topEngagementCourses; }
    public void setTopEngagementCourses(List<CourseAnalyticsDto> v) { this.topEngagementCourses = v; }

    public List<CourseAnalyticsDto> getTopRatedCourses() { return topRatedCourses; }
    public void setTopRatedCourses(List<CourseAnalyticsDto> v) { this.topRatedCourses = v; }

    public List<CourseAnalyticsDto> getRecommendedForFeatured() { return recommendedForFeatured; }
    public void setRecommendedForFeatured(List<CourseAnalyticsDto> v) { this.recommendedForFeatured = v; }

    public List<CourseAnalyticsDto> getNeedsImprovementCourses() { return needsImprovementCourses; }
    public void setNeedsImprovementCourses(List<CourseAnalyticsDto> v) { this.needsImprovementCourses = v; }
}
