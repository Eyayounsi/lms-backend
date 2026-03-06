package com.elearning.ProjetPfe.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

/**
 * Table "messages" — un message dans une conversation.
 */
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 10)
    private MessageType messageType = MessageType.TEXT;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }

    // ─── Getters & Setters ────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public MessageType getMessageType() { return messageType; }
    public void setMessageType(MessageType messageType) { this.messageType = messageType; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public LocalDateTime getSentAt() { return sentAt; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }
}
