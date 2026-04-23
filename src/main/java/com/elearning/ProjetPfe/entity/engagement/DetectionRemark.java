package com.elearning.ProjetPfe.entity.engagement;

import com.elearning.ProjetPfe.entity.course.Course;
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

@Entity
@Table(name = "detection_remarks")
public class DetectionRemark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "lesson_id")
    private Long lessonId;

    @Column(name = "remark_type", nullable = false, length = 50)
    private String remarkType;

    @Column(name = "remark_message", nullable = false, length = 500)
    private String remarkMessage;

    @Column(name = "attention_score")
    private Integer attentionScore;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @PrePersist
    public void prePersist() {
        if (detectedAt == null) detectedAt = LocalDateTime.now();
    }

    // ── Getters & Setters ──

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getStudent() { return student; }
    public void setStudent(User student) { this.student = student; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public Long getLessonId() { return lessonId; }
    public void setLessonId(Long lessonId) { this.lessonId = lessonId; }

    public String getRemarkType() { return remarkType; }
    public void setRemarkType(String remarkType) { this.remarkType = remarkType; }

    public String getRemarkMessage() { return remarkMessage; }
    public void setRemarkMessage(String remarkMessage) { this.remarkMessage = remarkMessage; }

    public Integer getAttentionScore() { return attentionScore; }
    public void setAttentionScore(Integer attentionScore) { this.attentionScore = attentionScore; }

    public LocalDateTime getDetectedAt() { return detectedAt; }
    public void setDetectedAt(LocalDateTime detectedAt) { this.detectedAt = detectedAt; }
}
