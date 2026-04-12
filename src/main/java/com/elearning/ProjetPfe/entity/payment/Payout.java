package com.elearning.ProjetPfe.entity.payment;

import com.elearning.ProjetPfe.entity.auth.User;
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
 * Table "payouts" — demandes de virement des instructors.
 *
 * Flux :
 *   1. Instructor demande un virement (status = PENDING)
 *   2. Admin approuve → status = PAID  + paidAt renseigné
 *   3. Admin rejette  → status = REJECTED + notes (raison)
 *
 * Le montant est calculé au moment de la demande :
 *   totalEarnings - alreadyPaidOrPending
 */
@Entity
@Table(name = "payouts")
public class Payout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** L'instructor qui demande le virement */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    /** Montant demandé (part nette instructor) */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * Période couverte par ce virement.
     * Format "YYYY-MM" pour un mois précis, ou "ALL" pour solde complet.
     */
    @Column(nullable = false, length = 20)
    private String period;

    /**
     * Statut du virement :
     *   PENDING  → en attente de traitement admin
     *   PAID     → virement effectué par l'admin
     *   REJECTED → refusé par l'admin
     */
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    /** Notes de l'admin (raison du rejet, confirmation, etc.) */
    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Date de la demande */
    @Column(nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    /** Date effective du virement (renseignée par l'admin) */
    private LocalDateTime paidAt;

    @PrePersist
    protected void onCreate() {
        requestedAt = LocalDateTime.now();
    }

    // ─── Constructeurs ────────────────────────────────────────────────────
    public Payout() {}

    // ─── Getters & Setters ────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getInstructor() { return instructor; }
    public void setInstructor(User instructor) { this.instructor = instructor; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getRequestedAt() { return requestedAt; }
    public void setRequestedAt(LocalDateTime requestedAt) { this.requestedAt = requestedAt; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}
