package com.elearning.ProjetPfe.repository.course;

import com.elearning.ProjetPfe.entity.course.Course;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.course.Section;

@Repository
public interface SectionRepository extends JpaRepository<Section, Long> {

    /** Toutes les sections d'un cours, triées par position */
    List<Section> findByCourseIdOrderByOrderIndexAsc(Long courseId);
}
