package com.elearning.ProjetPfe.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.LessonProgress;

/**
 * Repository pour la progression d'un étudiant dans une leçon.
 *
 * ─── REQUÊTES UTILES ──────────────────────────────────────────────────────
 *
 * findByStudentIdAndLessonId(studentId, lessonId)
 *   → récupère watchedSeconds pour reprendre la vidéo là où on l'a laissée
 *     Appelé quand l'étudiant ouvre une leçon.
 *
 * countCompletedInCourse(studentId, courseId)
 *   → nombre de leçons complétées dans un cours
 *     Utilisé avec LessonRepository.countByCourseId() pour calculer le %
 *
 *     % = countCompleted * 100.0 / totalLessons
 *
 * findByCourseIdAndStudentId(studentId, courseId)
 *   → toutes les progressions des leçons d'un cours
 *     Utilisé pour colorier chaque leçon (vert = terminé, orange = en cours)
 */
@Repository
public interface LessonProgressRepository extends JpaRepository<LessonProgress, Long> {

    /** Progression sur une leçon précise */
    Optional<LessonProgress> findByStudentIdAndLessonId(Long studentId, Long lessonId);

    /** Vérifie si une entrée existe (pour décider INSERT vs UPDATE) */
    boolean existsByStudentIdAndLessonId(Long studentId, Long lessonId);

    /**
     * Nombre de leçons complétées par l'étudiant dans un cours.
     *
     * Note: on joint via lesson.section.course pour accéder au course_id.
     * Spring Data JPA supporte ce join implicite via la syntaxe pointée.
     */
    @Query("SELECT COUNT(lp) FROM LessonProgress lp " +
           "WHERE lp.student.id = :studentId " +
           "AND lp.lesson.section.course.id = :courseId " +
           "AND lp.completed = true")
    long countCompletedInCourse(@Param("studentId") Long studentId,
                                @Param("courseId") Long courseId);

    /**
     * Toutes les progressions des leçons d'un cours pour un étudiant.
     * Utilisé pour afficher l'état de chaque leçon dans le curriculum.
     */
    @Query("SELECT lp FROM LessonProgress lp " +
           "WHERE lp.student.id = :studentId " +
           "AND lp.lesson.section.course.id = :courseId")
    List<LessonProgress> findByStudentIdAndCourseId(@Param("studentId") Long studentId,
                                                     @Param("courseId") Long courseId);

    /** Toutes les progressions de leçons d'une section (pour affichage en sidebar) */
    @Query("SELECT lp FROM LessonProgress lp " +
           "WHERE lp.student.id = :studentId " +
           "AND lp.lesson.section.id = :sectionId")
    List<LessonProgress> findByStudentIdAndSectionId(@Param("studentId") Long studentId,
                                                      @Param("sectionId") Long sectionId);

    /**
     * Supprime toutes les progressions de leçons appartenant à un cours.
     * Utilisé avant la suppression admin d'un cours.
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM LessonProgress lp WHERE lp.lesson.section.course.id = :courseId")
    void deleteByCourseId(@Param("courseId") Long courseId);

    /** Supprime toutes les progressions de leçons d'un étudiant (suppression de compte) */
    void deleteByStudentId(Long studentId);

    /** IDs des leçons complétées par l'étudiant dans un cours */
    @Query("SELECT lp.lesson.id FROM LessonProgress lp " +
           "WHERE lp.student.id = :studentId " +
           "AND lp.lesson.section.course.id = :courseId " +
           "AND lp.completed = true")
    List<Long> findCompletedLessonIds(@Param("studentId") Long studentId,
                                      @Param("courseId") Long courseId);

    /** Supprime toutes les progressions d'une leçon donnée */
    void deleteByLessonId(Long lessonId);
}
