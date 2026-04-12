package com.elearning.ProjetPfe.repository.course;

import com.elearning.ProjetPfe.entity.course.Course;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.course.Category;

/**
 * Repository pour les catégories.
 *
 * ─── REQUÊTES UTILES ───────────────────────────────────────────────────────
 *
 * findAll() → retourne toutes les catégories (triées par displayOrder côté service)
 * findBySlug(slug) → chercher une catégorie par son URL (ex: /categories/developpement-web)
 * existsByName(name) → vérifier qu'on ne crée pas de doublon
 * findByOrderByDisplayOrderAsc() → récupère les catégories dans l'ordre d'affichage
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** Cherche une catégorie par son slug unique (ex: "developpement-web") */
    Optional<Category> findBySlug(String slug);

    /** Vérifie si un nom existe déjà (pour éviter les doublons) */
    boolean existsByName(String name);

    /** Vérifie si un slug existe déjà */
    boolean existsBySlug(String slug);

    /**
     * Toutes les catégories triées par displayOrder ASC.
     * Utilisé pour l'affichage sur la page d'accueil.
     */
    List<Category> findAllByOrderByDisplayOrderAsc();
}
