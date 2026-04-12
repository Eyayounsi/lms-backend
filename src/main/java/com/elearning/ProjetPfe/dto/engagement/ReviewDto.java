package com.elearning.ProjetPfe.dto.engagement;

import java.time.LocalDateTime;

/**
 * DTO pour les avis sur un cours.
 *
 * ─── REQUÊTE POST (étudiant soumet un avis) ───────────────────────────────
 *   Champs requis : rating (1-5), comment (optionnel)
 *   courseId est pris depuis l'URL : POST /api/student/courses/{courseId}/reviews
 *
 * ─── RÉPONSE GET (liste des avis d'un cours) ──────────────────────────────
 *   Tous les champs sont remplis.
 *   studentName = firstName + " " + lastName
 */
public class ReviewDto {

    private Long id;

    /** 1, 2, 3, 4 ou 5 */
    private int rating;

    /** Texte du commentaire (peut être null) */
    private String comment;

    // ─── Info de l'étudiant (pour afficher "Par Jean Dupont") ─────────────
    private Long studentId;
    private String studentName;
    private String studentAvatar;

    /** ID du cours noté */
    private Long courseId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ReviewDto() {}

    // ─── Getters & Setters ────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getStudentAvatar() { return studentAvatar; }
    public void setStudentAvatar(String studentAvatar) { this.studentAvatar = studentAvatar; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
