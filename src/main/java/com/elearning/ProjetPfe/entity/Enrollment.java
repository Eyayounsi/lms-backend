package com.elearning.ProjetPfe.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Table "enrollments" — un étudiant inscrit à un cours (après paiement).
 *
 * Relations :
 *   - ManyToOne → User (l'étudiant)
 *   - ManyToOne → Course (le cours acheté)
 *
 * C'est cette table qui détermine si un student peut accéder au contenu.
 * Elle est créée uniquement après confirmation du paiement Stripe.
 */
@Entity
@Table(name = "enrollments", uniqueConstraints = {
    // Un étudiant ne peut pas acheter le même cours 2 fois
    @UniqueConstraint(columnNames = {"student_id", "course_id"})
})
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── L'étudiant ──────────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    // ─── Le cours acheté ─────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /** Identifiant de la session Stripe (pour traçabilité) */
    @Column(name = "stripe_session_id")
    private String stripeSessionId;

    /** Statut du paiement : PENDING, PAID, FAILED */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ─── Constructeurs ────────────────────────────────────────────────────
    public Enrollment() {}

    // ─── Getters & Setters ────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public String getStripeSessionId() { return stripeSessionId; }
    public void setStripeSessionId(String stripeSessionId) { this.stripeSessionId = stripeSessionId; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
