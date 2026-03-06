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
 * Table "cart_items" — panier d'achat d'un étudiant.
 *
 * ═══════════════════════════════════════════════════
 * RÈGLE MÉTIER :
 * ═══════════════════════════════════════════════════
 *   → Un cours ne peut apparaître qu'UNE FOIS dans le panier d'un étudiant.
 *     Contrainte UNIQUE sur (student_id, course_id).
 *
 *   → Quand l'étudiant confirme le paiement :
 *     1. On crée un Enrollment pour chaque CartItem
 *     2. On vide le panier (DELETE FROM cart_items WHERE student_id = ?)
 *
 *   → On ne met pas le prix ici : on lit course.getEffectivePrice()
 *     pour toujours avoir le prix actuel (avec promo si applicable).
 *
 * ═══════════════════════════════════════════════════
 * DIFFÉRENCE AVEC WISHLIST :
 * ═══════════════════════════════════════════════════
 *   Panier = intention d'achat immédiate → checkout Stripe
 *   Wishlist = "je veux ça plus tard" → pas de paiement direct
 */
@Entity
@Table(
    name = "cart_items",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_student_course_cart",
        columnNames = { "student_id", "course_id" }
    )
)
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── L'étudiant propriétaire du panier ───────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    // ─── Le cours ajouté au panier ───────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /** Date d'ajout au panier */
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @PrePersist
    public void prePersist() {
        this.addedAt = LocalDateTime.now();
    }

    // ─── Constructeur ─────────────────────────────────────────────────────
    public CartItem() {}

    // ─── Getters & Setters ────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public LocalDateTime getAddedAt() { return addedAt; }
}
