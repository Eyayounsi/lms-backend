package com.elearning.ProjetPfe.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour l'historique des commandes d'un étudiant.
 */
public class OrderHistoryDto {

    private Long enrollmentId;
    private Long courseId;
    private String courseTitle;
    private String courseCoverImage;
    private String instructorName;
    private BigDecimal amountPaid;
    private String paymentStatus;   // PENDING | PAID | FAILED
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

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

    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

