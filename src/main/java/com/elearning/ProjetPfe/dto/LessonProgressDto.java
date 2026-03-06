package com.elearning.ProjetPfe.dto;

import java.time.LocalDateTime;

/**
 * DTO pour sauvegarder et afficher la progression dans une leçon.
 *
 * ─── SAUVEGARDE AUTOMATIQUE (toutes les 30 secondes) ─────────────────────
 *
 *   Le frontend envoie :
 *     PUT /api/student/progress/lesson/{lessonId}
 *     Body: { "watchedSeconds": 245 }
 *
 *   Le backend :
 *     1. Upsert (INSERT ou UPDATE) le LessonProgress
 *     2. Si watchedSeconds >= lesson.durationSeconds * 0.8 → completed = true
 *     3. Recalcule CourseProgress.completionPercentage
 *     4. Retourne ce DTO avec isCompleted mis à jour
 *
 * ─── CHARGEMENT INITIAL ────────────────────────────────────────────────────
 *
 *   GET /api/student/progress/lesson/{lessonId}
 *   → retourne watchedSeconds pour que le player reprenne là où on en était
 */
public class LessonProgressDto {

    private Long lessonId;

    /**
     * Position de la vidéo en secondes.
     * Le frontend lit ce champ et fait : videoPlayer.currentTime = watchedSeconds;
     */
    private Long watchedSeconds;

    /** true si la leçon est complétée (≥ 80 % regardé ou "Marquer terminé") */
    private boolean completed;

    private LocalDateTime completedAt;
    private LocalDateTime updatedAt;

    public LessonProgressDto() {}

    // ─── Getters & Setters ────────────────────────────────────────────────
    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long lessonId) { this.lessonId = lessonId; }

    public Long getWatchedSeconds() { return watchedSeconds; }
    public void setWatchedSeconds(Long watchedSeconds) { this.watchedSeconds = watchedSeconds; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
