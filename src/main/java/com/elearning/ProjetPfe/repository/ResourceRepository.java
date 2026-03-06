package com.elearning.ProjetPfe.repository;

import com.elearning.ProjetPfe.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByLessonId(Long lessonId);
    void deleteAllByLessonId(Long lessonId);
}
