package com.elearning.ProjetPfe.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.CourseProgress;

/**
 * Repository pour la progression globale d'un étudiant dans un cours.
 *
 * ─── REQUÊTES UTILES ──────────────────────────────────────────────────────
 *
 * findByStudentIdAndCourseId(studentId, courseId)
 *   → récupère la progression pour afficher la barre de progression
 *     Ex: "62 % complété"
 *
 * findByStudentId(studentId)
 *   → tous les cours "en cours" d'un étudiant
 *     Utilisé dans "Mes cours" et le dashboard étudiant
 *
 * findCoursesInProgress(studentId)
 *   → cours entre 1% et 99% (filtre les "non-commencés" et "terminés")
 *
 * countCompleted(studentId)
 *   → nombre de cours terminés (certificats potentiels)
 */
@Repository
public interface CourseProgressRepository extends JpaRepository<CourseProgress, Long> {

    /** Progression pour une paire (étudiant, cours) */
    Optional<CourseProgress> findByStudentIdAndCourseId(Long studentId, Long courseId);

    /** Vérifie si la progression existe déjà */
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    /** Toutes les progressions d'un étudiant (pour "Mes cours") */
    List<CourseProgress> findByStudentIdOrderByLastAccessedAtDesc(Long studentId);

    /**
     * Cours en cours de lecture (entre 1 % et 99 %).
     * Affiché dans "Continuer la lecture" sur le dashboard.
     */
    @Query("SELECT cp FROM CourseProgress cp WHERE cp.student.id = :studentId " +
           "AND cp.completionPercentage > 0 AND cp.completionPercentage < 100 " +
           "ORDER BY cp.lastAccessedAt DESC")
    List<CourseProgress> findInProgressByStudentId(@Param("studentId") Long studentId);

    /**
     * Nombre de cours terminés à 100 % par l'étudiant.
     * Utilisé pour "X certificats obtenus".
     */
    @Query("SELECT COUNT(cp) FROM CourseProgress cp WHERE cp.student.id = :studentId " +
           "AND cp.completionPercentage = 100")
    long countCompletedByStudentId(@Param("studentId") Long studentId);

    /** Supprime toutes les progressions de cours pour un cours donné (suppression admin) */
    void deleteByCourseId(Long courseId);

    /** Supprime toutes les progressions d'un étudiant (suppression de compte) */
    void deleteByStudentId(Long studentId);
}
