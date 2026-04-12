package com.elearning.ProjetPfe.repository.course;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.course.Course;
import com.elearning.ProjetPfe.entity.course.CourseStatus;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

    /** Tous les cours d'un instructor donné */
    List<Course> findByInstructorId(Long instructorId);

    /** Tous les cours avec un statut donné (ex: PENDING pour l'admin, PUBLISHED pour les étudiants) */
    List<Course> findByStatus(CourseStatus status);

    /** Les cours d'un instructor filtrés par statut */
    List<Course> findByInstructorIdAndStatus(Long instructorId, CourseStatus status);

    /** Nombre de cours publiés dans une catégorie (pour CategoryDto.courseCount) */
    long countByCategoryIdAndStatus(Long categoryId, CourseStatus status);

    /** Tous les cours d'une catégorie avec un statut donné */
    List<Course> findByCategoryIdAndStatus(Long categoryId, CourseStatus status);

    /**
     * Recherche full-text dans le titre et la description.
     * Utilisé par la page publique : /api/public/courses?search=python
     */
    @Query("SELECT c FROM Course c WHERE c.status = 'PUBLISHED' AND " +
           "(LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Course> searchPublished(@Param("keyword") String keyword);

    /** Cours mis en avant (featured = true) et publiés */
    List<Course> findByStatusAndFeaturedTrue(CourseStatus status);

    /** Nombre de cours featured et publiés (pour la limite max featured) */
    long countByStatusAndFeaturedTrue(CourseStatus status);

    /**
     * Tous les cours SAUF ceux d'un statut donné.
     * Utilisé par l'admin pour voir tout sauf les DRAFT (brouillons privés de l'instructor).
     */
    List<Course> findByStatusNot(CourseStatus status);

    /** Cours avec modifications en attente (pour l'admin) */
    List<Course> findByHasPendingEdit(boolean hasPendingEdit);
}
