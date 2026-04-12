package com.elearning.ProjetPfe.repository.course;

import com.elearning.ProjetPfe.entity.course.Course;
import com.elearning.ProjetPfe.entity.course.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByLessonId(Long lessonId);
    void deleteAllByLessonId(Long lessonId);
}
