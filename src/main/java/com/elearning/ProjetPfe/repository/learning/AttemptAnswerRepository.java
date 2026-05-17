package com.elearning.ProjetPfe.repository.learning;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.learning.AttemptAnswer;

@Repository
public interface AttemptAnswerRepository extends JpaRepository<AttemptAnswer, Long> {

    void deleteByAttemptQuizCourseId(Long courseId);

    void deleteByAttemptQuizLessonSectionCourseId(Long courseId);
}
