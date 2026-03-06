package com.elearning.ProjetPfe.dto;

/**
 * DTO pour quand l'admin accepte ou rejette un cours.
 */
public class CourseReviewDto {
    private String action;          // "APPROVE" ou "REJECT"
    private String rejectionReason; // obligatoire si action = REJECT

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
