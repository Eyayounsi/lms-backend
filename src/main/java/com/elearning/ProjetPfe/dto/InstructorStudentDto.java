package com.elearning.ProjetPfe.dto;

import java.time.LocalDateTime;

/**
 * DTO pour afficher les infos d'un étudiant inscrit dans les cours de l'instructor.
 */
public class InstructorStudentDto {

    private Long enrollmentId;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private String studentAvatar;
    private Long courseId;
    private String courseTitle;
    private LocalDateTime enrolledAt;
    private Double completionPercentage;
    private boolean completed;
    private LocalDateTime completedAt;

    public InstructorStudentDto() {}

    public Long getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(Long enrollmentId) { this.enrollmentId = enrollmentId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    public String getStudentEmail() { return studentEmail; }
    public void setStudentEmail(String studentEmail) { this.studentEmail = studentEmail; }
    public String getStudentAvatar() { return studentAvatar; }
    public void setStudentAvatar(String studentAvatar) { this.studentAvatar = studentAvatar; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
    public LocalDateTime getEnrolledAt() { return enrolledAt; }
    public void setEnrolledAt(LocalDateTime enrolledAt) { this.enrolledAt = enrolledAt; }
    public Double getCompletionPercentage() { return completionPercentage; }
    public void setCompletionPercentage(Double completionPercentage) { this.completionPercentage = completionPercentage; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
