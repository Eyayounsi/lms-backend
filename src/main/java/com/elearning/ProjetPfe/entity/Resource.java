package com.elearning.ProjetPfe.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "resources")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceType type;

    @Column(nullable = false, length = 1000)
    private String url;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    public Resource() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ─── Getters & Setters ─────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Lesson getLesson() { return lesson; }
    public void setLesson(Lesson lesson) { this.lesson = lesson; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public ResourceType getType() { return type; }
    public void setType(ResourceType type) { this.type = type; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

