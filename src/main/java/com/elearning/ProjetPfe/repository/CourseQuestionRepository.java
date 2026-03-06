package com.elearning.ProjetPfe.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.CourseQuestion;

@Repository
public interface CourseQuestionRepository extends JpaRepository<CourseQuestion, Long> {

    List<CourseQuestion> findByCourseIdOrderByCreatedAtDesc(Long courseId);

    List<CourseQuestion> findByLessonIdOrderByCreatedAtDesc(Long lessonId);

    Optional<CourseQuestion> findByIdAndStudentId(Long id, Long studentId);
}
