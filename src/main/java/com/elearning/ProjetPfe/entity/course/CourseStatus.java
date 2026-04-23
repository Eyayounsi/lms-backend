package com.elearning.ProjetPfe.entity.course;

/**
 * Cycle de vie d'un cours :
 *
 *   DRAFT     → brouillon, l'instructor travaille dessus
 *               (seul l'instructor le voit)
 *
 *   PENDING   → soumis pour validation, en attente de décision admin
 *               (instructor ne peut plus modifier)
 *
 *   PUBLISHED → approuvé par admin, visible et achetable par les étudiants
 *
 *   REJECTED  → refusé par l'admin avec une raison
 *               (instructor peut corriger → repasse en DRAFT)
 *
 *   ARCHIVED  → retiré de la vente mais les étudiants déjà inscrits gardent l'accès
 *               (utile quand un cours est obsolète)
 *
 * Transitions autorisées :
 *   DRAFT    → PENDING   (instructor soumet)
 *   PENDING  → PUBLISHED (admin approuve)
 *   PENDING  → REJECTED  (admin rejette)
 *   REJECTED → DRAFT     (instructor remet en brouillon pour corriger)
 *   PUBLISHED→ ARCHIVED  (instructor ou admin archive)
 */
public enum CourseStatus {
    DRAFT,
    PENDING,
    PUBLISHED,
    REJECTED,
    ARCHIVED
}
