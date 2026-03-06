package com.elearning.ProjetPfe.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO pour afficher la progression globale dans un cours.
 *
 * ─── DASHBOARD ÉTUDIANT ────────────────────────────────────────────────────
 *
 *   GET /api/student/progress/course/{courseId}
 *   → retourne ce DTO pour afficher la barre de progression
 *
 *   GET /api/student/progress/my-courses
 *   → retourne une liste de ce DTO pour l'onglet "Mes cours"
 *
 * ─── CHAMP lastLessonId ────────────────────────────────────────────────────
 *
 *   Utilisé pour le bouton "Continuer la lecture" :
 *     router.navigate(['/course', courseId, 'lesson', lastLessonId]);
 *
 *   Mis à jour à chaque fois que l'étudiant ouvre une leçon.
 *
 * ─── CHAMP completionPercentage ────────────────────────────────────────────
 *
 *   Arrondi à 1 décimale (côté service) : 62.5
 *   Côté frontend : Math.round(completionPercentage) pour la barre de progression
 */
public class CourseProgressDto {

    private Long courseId;
    private String courseTitle;
    private String courseCoverImage;

    /** 0.0 à 100.0 */
    private Double completionPercentage;

    /** ID de la dernière leçon consultée (pour "Continuer") */
    private Long lastLessonId;

    private LocalDateTime lastAccessedAt;

    /** null si pas encore terminé */
    private LocalDateTime completedAt;

    /** true si completedAt != null */
    private boolean completed;

    /** IDs des leçons terminées — utilisé pour verrouillage séquentiel côté frontend */
    private List<Long> completedLessonIds;

    public CourseProgressDto() {}

    // ─── Getters & Setters ────────────────────────────────────────────────
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }

    public String getCourseCoverImage() { return courseCoverImage; }
    public void setCourseCoverImage(String courseCoverImage) { this.courseCoverImage = courseCoverImage; }

    public Double getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(Double completionPercentage) { this.completionPercentage = completionPercentage; }

    public Long getLastLessonId() { return lastLessonId; }
    public void setLastLessonId(Long lastLessonId) { this.lastLessonId = lastLessonId; }

    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public List<Long> getCompletedLessonIds() { return completedLessonIds; }
    public void setCompletedLessonIds(List<Long> completedLessonIds) { this.completedLessonIds = completedLessonIds; }
}
