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

/**
 * Table "notifications"
 *
 * Système centralisé de notifications internes.
 *
 * Types de notifications :
 *   PURCHASE_CONFIRMED   → étudiant : achat confirmé
 *   COURSE_APPROVED      → instructor : cours approuvé
 *   COURSE_REJECTED      → instructor : cours rejeté
 *   CERTIFICATE_ISSUED   → étudiant : certificat généré
 *   NEW_REVENUE          → instructor : nouveau revenu
 *
 * Règle sécurité : un utilisateur ne voit QUE ses propres notifications.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ─── Destinataire de la notification ─────────────────────────────────
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Type de notification (enum string pour lisibilité en DB) */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    /** Titre court affiché dans la cloche de notifications */
    @Column(nullable = false, length = 255)
    private String title;

    /** Message détaillé */
    @Column(columnDefinition = "TEXT")
    private String message;

    /**
     * Lien de redirection optionnel.
     * Ex: "/student/my-courses/5" pour aller directement au cours.
     */
    @Column(length = 500)
    private String link;

    /** false = non lue (badge rouge), true = lue */
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ─── Constructeurs ────────────────────────────────────────────────────
    public Notification() {}

    public Notification(User user, NotificationType type, String title, String message, String link) {
        this.user = user;
        this.type = type;
        this.title = title;
        this.message = message;
        this.link = link;
        this.read = false;
    }

    // ─── Getters & Setters ────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}

