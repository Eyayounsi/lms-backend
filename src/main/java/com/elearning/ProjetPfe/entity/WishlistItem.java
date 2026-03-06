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
 * Table "wishlist_items" — liste de souhaits d'un étudiant.
 *
 * ═══════════════════════════════════════════════════
 * RÈGLE MÉTIER :
 * ═══════════════════════════════════════════════════
 *   → Un cours ne peut apparaître qu'UNE FOIS dans la wishlist.
 *     Contrainte UNIQUE sur (student_id, course_id).
 *
 *   → L'étudiant peut déplacer un cours de la wishlist vers le panier.
 *     Côté backend : DELETE wishlist_item + INSERT cart_item.
 *
 *   → Quand un étudiant achète un cours, on supprime automatiquement
 *     le WishlistItem correspondant (si il existe).
 *
 * ═══════════════════════════════════════════════════
 * UTILITÉ POUR LE MARKETING :
 * ═══════════════════════════════════════════════════
 *   → Permet d'envoyer des rappels (email) quand un cours
 *     en wishlist passe en promo.
 *   → Admin peut voir les cours les plus souhaitables.
 */
@Entity
@Table(
    name = "wishlist_items",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_student_course_wishlist",
        columnNames = { "student_id", "course_id" }
    )
)
public class WishlistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── L'étudiant propriétaire de la wishlist ───────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    // ─── Le cours souhaité ────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /** Date d'ajout à la wishlist */
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @PrePersist
    public void prePersist() {
        this.addedAt = LocalDateTime.now();
    }

    // ─── Constructeur ─────────────────────────────────────────────────────
    public WishlistItem() {}

    // ─── Getters & Setters ────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public LocalDateTime getAddedAt() { return addedAt; }
}
