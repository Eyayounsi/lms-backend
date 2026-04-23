package com.elearning.ProjetPfe.dto.payment;

import java.math.BigDecimal;

/**
 * DTO pour l'historique des commandes d'un étudiant.
 */
public class OrderHistoryDto {

    private Long enrollmentId;
    private Long courseId;
    private String courseTitle;
    private String courseCoverImage;
    private String instructorName;
    private BigDecimal amount;       // montant payé effectif
    private String status;           // PENDING | PAID | FAILED
    private String purchaseDate;     // date d'achat formatée (yyyy-MM-dd HH:mm)

    // ─── Getters & Setters ────────────────────────────────────────────────
    public Long getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(Long enrollmentId) { this.enrollmentId = enrollmentId; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }

    public String getCourseCoverImage() { return courseCoverImage; }
    public void setCourseCoverImage(String courseCoverImage) { this.courseCoverImage = courseCoverImage; }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }
}

