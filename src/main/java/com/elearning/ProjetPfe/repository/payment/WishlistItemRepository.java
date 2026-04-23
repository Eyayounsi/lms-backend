package com.elearning.ProjetPfe.repository.payment;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.payment.WishlistItem;

/**
 * Repository pour la liste de souhaits.
 *
 * ─── REQUÊTES UTILES ──────────────────────────────────────────────────────
 *
 * findByStudentId(studentId)
 *   → toute la wishlist d'un étudiant
 *
 * existsByStudentIdAndCourseId(studentId, courseId)
 *   → vérifie si le coeur est déjà dans la wishlist
 *     (pour changer l'icône cœur : vide → plein)
 *
 * deleteByStudentIdAndCourseId(studentId, courseId)
 *   → retirer de la wishlist
 *
 * deleteByStudentIdAndCourseId est aussi appelé après un achat
 * (si le cours acheté était dans la wishlist, on le retire automatiquement)
 */
@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    /** Toute la wishlist d'un étudiant */
    List<WishlistItem> findByStudentId(Long studentId);

    /** Un item précis */
    Optional<WishlistItem> findByStudentIdAndCourseId(Long studentId, Long courseId);

    /** Vérifie si le cours est dans la wishlist (pour l'icône ♥) */
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    /** Retire un cours de la wishlist */
    void deleteByStudentIdAndCourseId(Long studentId, Long courseId);

    /** Nombre de cours dans la wishlist */
    long countByStudentId(Long studentId);

    /** Supprime tous les favoris contenant ce cours (suppression admin) */
    void deleteByCourseId(Long courseId);

    /** Vide toute la wishlist d'un étudiant (suppression de compte) */
    void deleteByStudentId(Long studentId);
}
