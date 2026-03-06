package com.elearning.ProjetPfe.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO du tableau de bord de revenus de l'instructor.
 */
public class InstructorRevenueDto {

    /** Revenu total net de toute l'histoire */
    private BigDecimal totalRevenue;

    /** Revenu du mois courant */
    private BigDecimal currentMonthRevenue;

    /** Nombre total d'étudiants uniques */
    private long totalStudents;

    /** Détail par cours */
    private List<CourseRevenueItem> perCourse;

    /** Historique mensuel */
    private List<MonthlyRevenueItem> monthly;

    // ─── Inner classes ────────────────────────────────────────────────────

    public static class CourseRevenueItem {
        private Long courseId;
        private String courseTitle;
        private BigDecimal revenue;

        public CourseRevenueItem() {}
        public CourseRevenueItem(Long courseId, String courseTitle, BigDecimal revenue) {
            this.courseId = courseId;
            this.courseTitle = courseTitle;
            this.revenue = revenue;
        }

        public Long getCourseId() { return courseId; }
        public void setCourseId(Long courseId) { this.courseId = courseId; }
        public String getCourseTitle() { return courseTitle; }
        public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
    }

    public static class MonthlyRevenueItem {
        private String month;      // "2026-02"
        private BigDecimal revenue;

        public MonthlyRevenueItem() {}
        public MonthlyRevenueItem(String month, BigDecimal revenue) {
            this.month = month;
            this.revenue = revenue;
        }

        public String getMonth() { return month; }
        public void setMonth(String month) { this.month = month; }
        public BigDecimal getRevenue() { return revenue; }
        public void setRevenue(BigDecimal revenue) { this.revenue = revenue; }
    }

    // ─── Getters & Setters ────────────────────────────────────────────────
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public BigDecimal getCurrentMonthRevenue() { return currentMonthRevenue; }
    public void setCurrentMonthRevenue(BigDecimal currentMonthRevenue) { this.currentMonthRevenue = currentMonthRevenue; }

    public long getTotalStudents() { return totalStudents; }
    public void setTotalStudents(long totalStudents) { this.totalStudents = totalStudents; }

    public List<CourseRevenueItem> getPerCourse() { return perCourse; }
    public void setPerCourse(List<CourseRevenueItem> perCourse) { this.perCourse = perCourse; }

    public List<MonthlyRevenueItem> getMonthly() { return monthly; }
    public void setMonthly(List<MonthlyRevenueItem> monthly) { this.monthly = monthly; }
}

