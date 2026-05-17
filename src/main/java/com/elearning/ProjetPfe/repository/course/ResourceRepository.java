package com.elearning.ProjetPfe.repository.course;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.elearning.ProjetPfe.entity.course.Resource;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByLessonId(Long lessonId);
    void deleteAllByLessonId(Long lessonId);
    void deleteAllByLessonSectionCourseId(Long courseId);
}
