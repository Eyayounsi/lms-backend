package com.elearning.ProjetPfe.dto;

import java.time.LocalDateTime;

/**
 * DTO retourné pour un certificat.
 * Utilisé pour la liste de l'étudiant ET pour la vérification publique.
 */
public class CertificateDto {

    private Long id;
    private Long studentId;
    private String studentName;
    private Long courseId;
    private String courseTitle;
    private String instructorName;
    private String certificateCode;
    private LocalDateTime issuedAt;

    // ─── Getters & Setters ────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

    public String getCertificateCode() { return certificateCode; }
    public void setCertificateCode(String certificateCode) { this.certificateCode = certificateCode; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
}

