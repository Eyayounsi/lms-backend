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
 * Table "certificates"
 *
 * Généré automatiquement quand un étudiant atteint 100% dans un cours.
 *
 * Règles :
 *  - 1 certificat par (étudiant, cours)
 *  - code unique sécurisé pour vérification publique
 *  - non générable si le cours est remboursé (paymentStatus != PAID)
 */
@Entity
@Table(
    name = "certificates",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_student_course_cert",
        columnNames = {"student_id", "course_id"}
    )
)
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── L'étudiant propriétaire du certificat ────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    // ─── Le cours complété ────────────────────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /**
     * Code unique de vérification publique (UUID sans tirets, 32 chars).
     * Permet à quiconque de vérifier l'authenticité du certificat
     * via GET /api/public/certificates/verify/{code}
     */
    @Column(name = "certificate_code", nullable = false, unique = true, length = 64)
    private String certificateCode;

    /** Date d'émission du certificat */
    @Column(name = "issued_at", nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @PrePersist
    protected void onCreate() {
        issuedAt = LocalDateTime.now();
    }

    // ─── Constructeurs ────────────────────────────────────────────────────
    public Certificate() {}

    // ─── Getters & Setters ────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public String getCertificateCode() { return certificateCode; }
    public void setCertificateCode(String certificateCode) { this.certificateCode = certificateCode; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
}

