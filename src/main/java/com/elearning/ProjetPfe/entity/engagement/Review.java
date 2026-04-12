package com.elearning.ProjetPfe.entity.engagement;

import com.elearning.ProjetPfe.service.engagement.ReviewService;
import com.elearning.ProjetPfe.service.course.CourseService;
import com.elearning.ProjetPfe.entity.learning.Note;
import com.elearning.ProjetPfe.entity.payment.Enrollment;
import com.elearning.ProjetPfe.entity.course.Course;
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
 * Table "reviews" — avis d'un étudiant sur un cours.
 *
 * ═══════════════════════════════════════════════════
 * RÈGLE MÉTIER IMPORTANTE :
 * ═══════════════════════════════════════════════════
 *   Un étudiant ne peut écrire qu'UN SEUL avis par cours.
 *   Contrainte UNIQUE sur (student_id, course_id).
 *
 * ═══════════════════════════════════════════════════
 * QUI PEUT LAISSER UN AVIS ?
 * ═══════════════════════════════════════════════════
 *   Seuls les étudiants qui ont ACHETÉ le cours peuvent écrire un avis.
 *   On vérifie côté backend qu'il existe un Enrollment pour ce student+course.
 *
 * ═══════════════════════════════════════════════════
 * RATING :
 * ═══════════════════════════════════════════════════
 *   Entre 1 et 5 étoiles (validation côté service).
 *   La note moyenne du cours sera calculée dans CourseService
 *   avec : SELECT AVG(rating) FROM reviews WHERE course_id = ?
 */
@Entity
@Table(
    name = "reviews",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_student_course_review",
        columnNames = { "student_id", "course_id" }
    )
)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── L'étudiant qui a écrit cet avis ──────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    // ─── Le cours noté ────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /**
     * Note de 1 à 5.
     * La validation min/max est faite dans ReviewService avant de sauvegarder.
     */
    @Column(nullable = false)
    private int rating;

    /** Commentaire de l'étudiant (optionnel) */
    @Column(columnDefinition = "TEXT")
    private String comment;

    /** Date de publication de l'avis */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Date de la dernière modification (si l'étudiant modifie son avis) */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ─── Hooks JPA ────────────────────────────────────────────────────────

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ─── Constructeur ─────────────────────────────────────────────────────
    public Review() {}

    // ─── Getters & Setters ────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
