package com.elearning.ProjetPfe.entity;

import java.math.BigDecimal;
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
 * Table "instructor_revenues"
 *
 * Enregistre chaque transaction de revenu (ou remboursement) pour un instructor.
 *
 * À chaque paiement confirmé (webhook Stripe) :
 *   - Montant total = prix payé par l'étudiant
 *   - Commission plateforme = montant * platformCommissionRate
 *   - Part instructor = montant - commission
 *
 * En cas de remboursement : type = REFUND avec montants négatifs.
 */
@Entity
@Table(name = "instructor_revenues")
public class InstructorRevenue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── L'instructor concerné ────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    // ─── Le cours vendu ───────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // ─── L'enrollment lié au paiement ────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    /** Montant total payé par l'étudiant (en EUR) */
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * Taux de commission de la plateforme (ex: 0.30 = 30%).
     * Stocké par ligne pour garder l'historique même si le taux change.
     */
    @Column(name = "platform_commission_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal platformCommissionRate;

    /** Montant de la commission plateforme = totalAmount * platformCommissionRate */
    @Column(name = "platform_commission", nullable = false, precision = 10, scale = 2)
    private BigDecimal platformCommission;

    /** Part nette de l'instructor = totalAmount - platformCommission */
    @Column(name = "instructor_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal instructorAmount;

    /**
     * Type de transaction :
     *   SALE    = vente normale
     *   REFUND  = remboursement (montants négatifs)
     */
    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType = "SALE";

    /** Mois de la transaction au format "YYYY-MM" pour les stats mensuelles */
    @Column(name = "revenue_month", nullable = false, length = 7)
    private String revenueMonth;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ─── Constructeurs ────────────────────────────────────────────────────
    public InstructorRevenue() {}

    // ─── Getters & Setters ────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getInstructor() { return instructor; }
    public void setInstructor(User instructor) { this.instructor = instructor; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public Enrollment getEnrollment() { return enrollment; }
    public void setEnrollment(Enrollment enrollment) { this.enrollment = enrollment; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getPlatformCommissionRate() { return platformCommissionRate; }
    public void setPlatformCommissionRate(BigDecimal platformCommissionRate) {
        this.platformCommissionRate = platformCommissionRate;
    }

    public BigDecimal getPlatformCommission() { return platformCommission; }
    public void setPlatformCommission(BigDecimal platformCommission) {
        this.platformCommission = platformCommission;
    }

    public BigDecimal getInstructorAmount() { return instructorAmount; }
    public void setInstructorAmount(BigDecimal instructorAmount) {
        this.instructorAmount = instructorAmount;
    }

    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }

    public String getRevenueMonth() { return revenueMonth; }
    public void setRevenueMonth(String revenueMonth) { this.revenueMonth = revenueMonth; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}

