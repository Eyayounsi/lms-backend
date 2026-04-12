package com.elearning.ProjetPfe.repository.course;

import com.elearning.ProjetPfe.service.learning.ProgressService;
import com.elearning.ProjetPfe.entity.course.Section;
import com.elearning.ProjetPfe.entity.course.Course;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.course.Lesson;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    /** Toutes les leçons d'une section, triées par position */
    List<Lesson> findBySectionIdOrderByOrderIndexAsc(Long sectionId);

    /**
     * Nombre total de leçons dans un cours.
     * Spring Data résout le chemin : lesson → section → course
     * via la syntaxe pointée : section_course_id = courseId
     *
     * Utilisé par ProgressService pour calculer le % d'avancement.
     */
    long countBySectionCourseId(Long courseId);
}
