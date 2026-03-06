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

/**
 * Table "course_answers" — réponse à une question Q&A.
 */
@Entity
@Table(name = "course_answers")
public class CourseAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "is_instructor_answer", nullable = false)
    private boolean instructorAnswer = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private CourseQuestion question;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ─── Getters & Setters ────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public boolean isInstructorAnswer() { return instructorAnswer; }
    public void setInstructorAnswer(boolean instructorAnswer) { this.instructorAnswer = instructorAnswer; }

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }

    public CourseQuestion getQuestion() { return question; }
    public void setQuestion(CourseQuestion question) { this.question = question; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
