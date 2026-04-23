package com.elearning.ProjetPfe.dto.admin;

import com.elearning.ProjetPfe.entity.admin.Ticket;
import com.elearning.ProjetPfe.entity.course.Category;
/**
 * DTO retourné au frontend pour un ticket de support.
 */
public class TicketDto {

    private Long id;
    /** Identifiant lisible ex: "TIC001" */
    private String ticketId;
    private String date;
    private String subject;
    private String category;
    private String priority;
    private String status;
    private String description;
    private String userName;
    private String userEmail;
    private String adminReply;
    private String respondedAt;

    // ─── Getters / Setters ────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTicketId() { return ticketId; }
    public void setTicketId(String ticketId) { this.ticketId = ticketId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getAdminReply() { return adminReply; }
    public void setAdminReply(String adminReply) { this.adminReply = adminReply; }

    public String getRespondedAt() { return respondedAt; }
    public void setRespondedAt(String respondedAt) { this.respondedAt = respondedAt; }
}
