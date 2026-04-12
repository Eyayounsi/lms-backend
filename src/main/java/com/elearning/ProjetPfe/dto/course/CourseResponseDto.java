package com.elearning.ProjetPfe.dto.course;

import com.elearning.ProjetPfe.entity.course.Course;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO pour AFFICHER un cours (ce que le frontend reçoit).
 *
 * ═══════════════════════════════════════════════════
 * CHAMPS IMPORTANTS :
 * ═══════════════════════════════════════════════════
 *
 * effectivePrice : prix réel à payer (avec promo si applicable)
 *   → calculé par course.getEffectivePrice() dans le service
 *   → le frontend TOUJOURS affiche effectivePrice, jamais price seul
 *
 * onSale : true si une promo est active
 *   → used pour barrer le prix original et afficher "PROMO !"
 *
 * averageRating, reviewCount : calculés à la volée par le service
 *   SELECT AVG(rating), COUNT(*) FROM reviews WHERE course_id = ?
 *
 * ═══════════════════════════════════════════════════
 * SECTIONS vs SECTIONS PUBLIC :
 * ═══════════════════════════════════════════════════
 *   - sections : curriculum complet (instructor + admin + étudiant inscrit)
 *   - Pour la page publique, on expose les sections mais on cache
 *     le videoUrl des leçons non-gratuites
 */
public class CourseResponseDto {

    // ─── Infos de base ────────────────────────────────────────────────────
    private Long id;
    private String title;
    private String description;
    private String objectives;
    private String requirements;
    private String language;

    // ─── Catégorie ────────────────────────────────────────────────────────
    private Long categoryId;
    private String categoryName;

    // ─── Médias ───────────────────────────────────────────────────────────
    private String coverImage;

    // ─── Prix ─────────────────────────────────────────────────────────────
    private BigDecimal price;
    private BigDecimal discountPrice;
    private LocalDateTime discountEndsAt;
    /** Prix réel à payer (= discountPrice si promo active, sinon price) */
    private BigDecimal effectivePrice;
    /** true si discountPrice est défini ET (discountEndsAt est null OU dans le futur) */
    private boolean onSale;

    // ─── Statistiques ─────────────────────────────────────────────────────
    private Long totalDurationSeconds;
    private Double averageRating;   // null si aucun avis
    private Long reviewCount;       // 0 si aucun avis
    private Long enrollmentCount;   // nombre d'étudiants inscrits (PAID)

    // ─── Niveau et état ───────────────────────────────────────────────────
    private String level;
    private String status;
    private String rejectionReason;
    private boolean featured;
    private LocalDateTime publishedAt;

    // ─── Instructor ───────────────────────────────────────────────────────
    private Long instructorId;
    private String instructorName;
    private String instructorAvatar;

    // ─── Dates ────────────────────────────────────────────────────────────
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ─── Curriculum ───────────────────────────────────────────────────────
    private List<SectionDto> sections;

    // ─── Versioning (modification en attente) ─────────────────────────────
    /** true si l'instructor a soumis une modification en attente */
    private boolean hasPendingEdit;
    /** JSON parsé des modifications en attente (pour l'admin) */
    private Map<String, Object> pendingEditData;
    /** Raison du rejet de la modification (si applicable) */
    private String editRejectionReason;

    // ─── Constructeur ─────────────────────────────────────────────────────
    public CourseResponseDto() {}

    // ─── Getters & Setters ────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getObjectives() { return objectives; }
    public void setObjectives(String objectives) { this.objectives = objectives; }

    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getDiscountPrice() { return discountPrice; }
    public void setDiscountPrice(BigDecimal discountPrice) { this.discountPrice = discountPrice; }

    public LocalDateTime getDiscountEndsAt() { return discountEndsAt; }
    public void setDiscountEndsAt(LocalDateTime discountEndsAt) { this.discountEndsAt = discountEndsAt; }

    public BigDecimal getEffectivePrice() { return effectivePrice; }
    public void setEffectivePrice(BigDecimal effectivePrice) { this.effectivePrice = effectivePrice; }

    public boolean isOnSale() { return onSale; }
    public void setOnSale(boolean onSale) { this.onSale = onSale; }

    public Long getTotalDurationSeconds() { return totalDurationSeconds; }
    public void setTotalDurationSeconds(Long totalDurationSeconds) { this.totalDurationSeconds = totalDurationSeconds; }

    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public Long getReviewCount() { return reviewCount; }
    public void setReviewCount(Long reviewCount) { this.reviewCount = reviewCount; }

    public Long getEnrollmentCount() { return enrollmentCount; }
    public void setEnrollmentCount(Long enrollmentCount) { this.enrollmentCount = enrollmentCount; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }

    public Long getInstructorId() { return instructorId; }
    public void setInstructorId(Long instructorId) { this.instructorId = instructorId; }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

    public String getInstructorAvatar() { return instructorAvatar; }
    public void setInstructorAvatar(String instructorAvatar) { this.instructorAvatar = instructorAvatar; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<SectionDto> getSections() { return sections; }
    public void setSections(List<SectionDto> sections) { this.sections = sections; }

    public boolean isHasPendingEdit() { return hasPendingEdit; }
    public void setHasPendingEdit(boolean hasPendingEdit) { this.hasPendingEdit = hasPendingEdit; }

    public Map<String, Object> getPendingEditData() { return pendingEditData; }
    public void setPendingEditData(Map<String, Object> pendingEditData) { this.pendingEditData = pendingEditData; }

    public String getEditRejectionReason() { return editRejectionReason; }
    public void setEditRejectionReason(String editRejectionReason) { this.editRejectionReason = editRejectionReason; }
}
