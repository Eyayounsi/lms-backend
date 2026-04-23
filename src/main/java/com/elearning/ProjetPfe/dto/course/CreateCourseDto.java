package com.elearning.ProjetPfe.dto.course;

import com.elearning.ProjetPfe.entity.course.Course;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO pour CRÉER ou MODIFIER un cours (ce que le frontend envoie).
 *
 * ═══════════════════════════════════════════════════
 * WIZARD EN 3 ÉTAPES (frontend) :
 * ═══════════════════════════════════════════════════
 *   Étape 1 - Infos de base : title, description, level, language, categoryId
 *   Étape 2 - Contenu : objectives, requirements (sections/leçons séparément)
 *   Étape 3 - Prix : price, discountPrice, discountEndsAt
 *
 * Le frontend peut envoyer le tout en une seule requête POST ou
 * sauvegarder étape par étape avec PATCH.
 */
public class CreateCourseDto {

    // ─── Étape 1 ──────────────────────────────────────────────────────────
    private String title;
    private String description;
    private String level;         // "BEGINNER", "INTERMEDIATE", "ADVANCED"
    private String language;      // ex: "Français", "English"
    private Long categoryId;      // ID de la catégorie choisie

    // ─── Étape 2 ──────────────────────────────────────────────────────────
    /** Ce que l'étudiant saura faire à la fin (liste séparée par \n) */
    private String objectives;

    /** Prérequis (ce qu'il faut savoir avant de commencer) */
    private String requirements;

    // ─── Étape 3 ──────────────────────────────────────────────────────────
    private BigDecimal price;

    /** Prix promotionnel (null = pas de promo) */
    private BigDecimal discountPrice;

    /** Date de fin de la promotion (null = illimitée si discountPrice non null) */
    private LocalDateTime discountEndsAt;

    // ─── Getters & Setters ────────────────────────────────────────────────
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getObjectives() { return objectives; }
    public void setObjectives(String objectives) { this.objectives = objectives; }

    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getDiscountPrice() { return discountPrice; }
    public void setDiscountPrice(BigDecimal discountPrice) { this.discountPrice = discountPrice; }

    public LocalDateTime getDiscountEndsAt() { return discountEndsAt; }
    public void setDiscountEndsAt(LocalDateTime discountEndsAt) { this.discountEndsAt = discountEndsAt; }
}
