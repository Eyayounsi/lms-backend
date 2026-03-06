package com.elearning.ProjetPfe.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

/**
 * Table "conversations" — conversation entre deux utilisateurs.
 */
@Entity
@Table(name = "conversations",
       uniqueConstraints = @UniqueConstraint(columnNames = {"participant1_id", "participant2_id"}))
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant1_id", nullable = false)
    private User participant1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant2_id", nullable = false)
    private User participant2;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sentAt ASC")
    private List<Message> messages = new ArrayList<>();

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastMessageAt = LocalDateTime.now();
    }

    // ─── Helper ───────────────────────────────────────────────────────────
    /** Retourne l'autre participant */
    public User getOtherParticipant(User me) {
        return participant1.getId().equals(me.getId()) ? participant2 : participant1;
    }

    // ─── Getters & Setters ────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getParticipant1() { return participant1; }
    public void setParticipant1(User participant1) { this.participant1 = participant1; }

    public User getParticipant2() { return participant2; }
    public void setParticipant2(User participant2) { this.participant2 = participant2; }

    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }

    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
