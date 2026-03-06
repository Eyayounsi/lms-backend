package com.elearning.ProjetPfe.repository;

import com.elearning.ProjetPfe.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour le panier d'achat.
 *
 * ─── REQUÊTES UTILES ──────────────────────────────────────────────────────
 *
 * findByStudentId(studentId)
 *   → tous les articles du panier d'un étudiant
 *     (utilisé pour afficher le panier ET pour le checkout Stripe)
 *
 * findByStudentIdAndCourseId(studentId, courseId)
 *   → vérifie si un cours est déjà dans le panier
 *     (avant d'ajouter, pour éviter les doublons — la DB rejette aussi via UNIQUE)
 *
 * deleteByStudentIdAndCourseId(studentId, courseId)
 *   → supprime un article spécifique du panier (bouton "Retirer")
 *
 * deleteByStudentId(studentId)
 *   → vide tout le panier après un paiement réussi
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /** Tout le panier d'un étudiant */
    List<CartItem> findByStudentId(Long studentId);

    /** Un article précis du panier */
    Optional<CartItem> findByStudentIdAndCourseId(Long studentId, Long courseId);

    /** Vérifie si le cours est déjà dans le panier */
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    /** Supprime un article du panier */
    void deleteByStudentIdAndCourseId(Long studentId, Long courseId);

    /** Vide tout le panier (appelé après paiement réussi) */
    void deleteByStudentId(Long studentId);

    /** Nombre d'articles dans le panier (pour le badge du header) */
    long countByStudentId(Long studentId);

    /** Supprime tous les paniers contenant ce cours (suppression admin) */
    void deleteByCourseId(Long courseId);
}
