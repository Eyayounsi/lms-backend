package com.elearning.ProjetPfe.entity.payment;

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
import jakarta.persistence.Table;

/**
 * Coupon généré lorsqu'un étudiant échange ses points.
 */
@Entity
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    /** Code unique du coupon (affiché à l'étudiant) */
    @Column(name = "coupon_code", nullable = false, unique = true, length = 30)
    private String couponCode;

    /** Points dépensés pour ce coupon */
    @Column(name = "points_spent", nullable = false)
    private int pointsSpent;

    /** Pourcentage de réduction */
    @Column(name = "discount_percent", nullable = false)
    private int discountPercent;

    /** Utilisé ou non */
    @Column(nullable = false)
    private boolean used = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Date d'expiration (10 jours après création) */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (expiresAt == null) {
            expiresAt = createdAt.plusDays(10);
        }
    }

    /** Vérifie si le coupon est expiré */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Statut calculé : Utilisé > Expiré > Actif
     */
    public String getStatus() {
        if (used) return "Utilisé";
        if (isExpired()) return "Expiré";
        return "Actif";
    }

    // ─── Getters & Setters ────────────────────────────────

    public Long getId() { return id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }

    public int getPointsSpent() { return pointsSpent; }
    public void setPointsSpent(int pointsSpent) { this.pointsSpent = pointsSpent; }

    public int getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(int discountPercent) { this.discountPercent = discountPercent; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
