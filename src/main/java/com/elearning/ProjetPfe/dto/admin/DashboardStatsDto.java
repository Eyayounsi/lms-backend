package com.elearning.ProjetPfe.dto.admin;

import java.util.Map;

/**
 * DTO contenant toutes les statistiques pour le tableau de bord.
 */
public class DashboardStatsDto {

    private long totalUsers;
    private long activeUsers;
    private long blockedUsers;

    // Clé = nom du rôle (STUDENT, INSTRUCTOR, ADMIN, RECRUITER, SUPERADMIN)
    private Map<String, Long> usersByRole;

    // Clé = "YYYY-MM" (ex: "2026-01"), valeur = nombre d'inscriptions ce mois
    private Map<String, Long> registrationsByMonth;

    // Getters & Setters
    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getActiveUsers() { return activeUsers; }
    public void setActiveUsers(long activeUsers) { this.activeUsers = activeUsers; }

    public long getBlockedUsers() { return blockedUsers; }
    public void setBlockedUsers(long blockedUsers) { this.blockedUsers = blockedUsers; }

    public Map<String, Long> getUsersByRole() { return usersByRole; }
    public void setUsersByRole(Map<String, Long> usersByRole) { this.usersByRole = usersByRole; }

    public Map<String, Long> getRegistrationsByMonth() { return registrationsByMonth; }
    public void setRegistrationsByMonth(Map<String, Long> registrationsByMonth) {
        this.registrationsByMonth = registrationsByMonth;
    }
}
