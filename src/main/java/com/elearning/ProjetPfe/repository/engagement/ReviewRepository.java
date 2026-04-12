package com.elearning.ProjetPfe.repository.engagement;

import com.elearning.ProjetPfe.entity.learning.Note;
import com.elearning.ProjetPfe.entity.course.Course;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.engagement.Review;

/**
 * Repository pour les avis (notes) sur les cours.
 *
 * ─── REQUÊTES UTILES ──────────────────────────────────────────────────────
 *
 * findByCourseId(courseId)
 *   → tous les avis d'un cours (pour l'affichage public)
 *
 * findByStudentIdAndCourseId(studentId, courseId)
 *   → vérifie si l'étudiant a déjà laissé un avis
 *     (pour afficher son avis existant OU autoriser la création)
 *
 * calculateAverageRating(courseId)
 *   → note moyenne du cours (/5) — affiché avec les étoiles
 *
 * countByCourseId(courseId)
 *   → nombre total d'avis (pour afficher "4.5 ★ (128 avis)")
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** Tous les avis pour un cours donné (ordre chronologique inversé) */
    List<Review> findByCourseIdOrderByCreatedAtDesc(Long courseId);

    /** L'avis d'un étudiant pour un cours précis (null si pas encore noté) */
    Optional<Review> findByStudentIdAndCourseId(Long studentId, Long courseId);

    /** Indique si l'étudiant a déjà laissé un avis sur ce cours */
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    /** Nombre d'avis sur un cours */
    long countByCourseId(Long courseId);

    /** Supprime tous les avis d'un cours (suppression admin) */
    void deleteByCourseId(Long courseId);

    /** Supprime tous les avis d'un étudiant (suppression de compte) */
    void deleteByStudentId(Long studentId);

    /** Nombre total d'avis écrits par un étudiant */
    long countByStudentId(Long studentId);

    /**
     * Note moyenne d'un cours.
     * Retourne null si aucun avis (course.averageRating = null → affiché "Pas encore noté")
     *
     * Exemple de résultat : 4.3
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.course.id = :courseId")
    Double calculateAverageRating(@Param("courseId") Long courseId);
}
