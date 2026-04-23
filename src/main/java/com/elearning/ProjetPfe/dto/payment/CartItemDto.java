package com.elearning.ProjetPfe.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour afficher un article du panier.
 *
 * Le total du panier = somme de tous les effectivePrice.
 * Calculé côté frontend : cart.reduce((sum, item) => sum + item.effectivePrice, 0)
 */
public class CartItemDto {

    private Long id;

    // ─── Infos du cours ───────────────────────────────────────────────────
    private Long courseId;
    private String courseTitle;
    private String courseCoverImage;
    private String instructorName;

    /** Prix original */
    private BigDecimal originalPrice;

    /** Prix réel à payer (avec promotion si applicable) */
    private BigDecimal effectivePrice;

    /** true si le cours est en promo */
    private boolean onSale;

    private LocalDateTime addedAt;

    public CartItemDto() {}

    // ─── Getters & Setters ────────────────────────────────────────────────
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }

    public String getCourseTitle() { return courseTitle; }
    public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }

    public String getCourseCoverImage() { return courseCoverImage; }
    public void setCourseCoverImage(String courseCoverImage) { this.courseCoverImage = courseCoverImage; }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }

    public BigDecimal getEffectivePrice() { return effectivePrice; }
    public void setEffectivePrice(BigDecimal effectivePrice) { this.effectivePrice = effectivePrice; }

    public boolean isOnSale() { return onSale; }
    public void setOnSale(boolean onSale) { this.onSale = onSale; }

    public LocalDateTime getAddedAt() { return addedAt; }
    public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }
}
