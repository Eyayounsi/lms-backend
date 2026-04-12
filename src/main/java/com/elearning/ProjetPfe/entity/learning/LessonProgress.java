package com.elearning.ProjetPfe.entity.learning;

import com.elearning.ProjetPfe.entity.course.Lesson;
import com.elearning.ProjetPfe.entity.auth.User;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Table "lesson_progress" — position exacte d'un étudiant dans une leçon.
 *
 * ═══════════════════════════════════════════════════
 * FONCTIONNALITÉ CLÉ : "Reprendre où j'en étais"
 * ═══════════════════════════════════════════════════
 *   Le frontend envoie une requête toutes les 30 secondes pendant la lecture :
 *
 *     PUT /api/student/progress/lesson/{lessonId}
 *     { "watchedSeconds": 245 }
 *
 *   On sauvegarde watchedSeconds ici.
 *   Quand l'étudiant revient, on charge watchedSeconds → le player reprend à 245s.
 *
 * ═══════════════════════════════════════════════════
 * QUAND UNE LEÇON EST CONSIDÉRÉE "COMPLÉTÉE" ?
 * ═══════════════════════════════════════════════════
 *   Règle : watchedSeconds >= (lesson.durationSeconds * 0.8)
 *   = l'étudiant a regardé au moins 80 % de la vidéo.
 *
 *   Quand completed = true :
 *     → On marque completedAt
 *     → On recalcule CourseProgress.completionPercentage
 *
 * ═══════════════════════════════════════════════════
 * POUR LES LEÇONS TEXT/PDF :
 * ═══════════════════════════════════════════════════
 *   watchedSeconds = 0 (pas pertinent)
 *   completed est mis à true quand l'étudiant clique "Marquer comme terminé"
 *
 * ═══════════════════════════════════════════════════
 * updatedAt :
 * ═══════════════════════════════════════════════════
 *   Mis à jour à chaque sauvegarde automatique (toutes les 30s).
 *   Permet de voir "Regardé il y a 5 min" dans le dashboard.
 */
@Entity
@Table(
    name = "lesson_progress",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_student_lesson_progress",
        columnNames = { "student_id", "lesson_id" }
    )
)
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── L'étudiant ───────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    // ─── La leçon spécifique ──────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    /**
     * Position exacte dans la vidéo, en secondes.
     * Ex: 245 signifie que l'étudiant a regardé jusqu'à 4min 05sec.
     * Pour les leçons TEXT/PDF, cette valeur reste 0.
     */
    @Column(name = "watched_seconds", nullable = false)
    private Long watchedSeconds = 0L;

    /**
     * true = la leçon est terminée (a regardé 80 %+ ou a cliqué "terminé").
     * Une fois completed = true, on ne le remet pas à false.
     */
    @Column(name = "is_completed", nullable = false)
    private boolean completed = false;

    /** null tant que la leçon n'est pas terminée */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** Mis à jour à chaque sauvegarde automatique depuis le video player */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ─── Constructeur ─────────────────────────────────────────────────────
    public LessonProgress() {}

    // ─── Getters & Setters ────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public Lesson getLesson() { return lesson; }
    public void setLesson(Lesson lesson) { this.lesson = lesson; }

    public Long getWatchedSeconds() { return watchedSeconds; }
    public void setWatchedSeconds(Long watchedSeconds) { this.watchedSeconds = watchedSeconds; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
