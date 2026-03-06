package com.elearning.ProjetPfe.entity;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Table "course_progress" — progression globale d'un étudiant dans un cours.
 *
 * ═══════════════════════════════════════════════════
 * COMMENT ÇA MARCHE ?
 * ═══════════════════════════════════════════════════
 *   1. Un étudiant achète le cours → on crée un CourseProgress (0 %)
 *   2. Il regarde une leçon → on met à jour LessonProgress
 *   3. Quand une leçon est viewée à 80 % → on la marque comme "completed"
 *   4. completionPercentage = (lessons complétées / total lessons) * 100
 *   5. Quand completionPercentage = 100 → on remplit completedAt
 *
 * ═══════════════════════════════════════════════════
 * lastLessonId :
 * ═══════════════════════════════════════════════════
 *   Stocke l'ID de la DERNIÈRE leçon consultée.
 *   Permet le bouton "Continuer là où j'en étais" sur le dashboard.
 *
 * ═══════════════════════════════════════════════════
 * RECALCUL DE completionPercentage :
 * ═══════════════════════════════════════════════════
 *   Se fait dans ProgressService.recalculate(studentId, courseId) :
 *
 *   long total = lessonRepo.countByCourseId(courseId);
 *   long done  = lessonProgressRepo.countCompleted(studentId, courseId);
 *   double pct = total == 0 ? 0 : (done * 100.0) / total;
 */
@Entity
@Table(
    name = "course_progress",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_student_course_progress",
        columnNames = { "student_id", "course_id" }
    )
)
public class CourseProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── L'étudiant ───────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    // ─── Le cours concerné ────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /**
     * Pourcentage d'avancement : 0.0 → 100.0
     * Recalculé à chaque fois qu'une leçon est complétée.
     */
    @Column(name = "completion_percentage", nullable = false)
    private Double completionPercentage = 0.0;

    /**
     * ID de la dernière leçon consultée → pour "Continuer".
     * Nullable car au départ l'étudiant n'a regardé aucune leçon.
     */
    @Column(name = "last_lesson_id")
    private Long lastLessonId;

    /** Dernière fois que l'étudiant a ouvert ce cours */
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    /** null tant que le cours n'est pas terminé (100 %) */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** Date de création (= date d'achat du cours) */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        lastAccessedAt = LocalDateTime.now();
    }

    // ─── Constructeur ─────────────────────────────────────────────────────
    public CourseProgress() {}

    // ─── Getters & Setters ────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public Double getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(Double completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public Long getLastLessonId() { return lastLessonId; }
    public void setLastLessonId(Long lastLessonId) { this.lastLessonId = lastLessonId; }

    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
