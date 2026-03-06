package com.elearning.ProjetPfe.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.QuizAttempt;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByStudentIdAndQuizIdOrderByStartedAtDesc(Long studentId, Long quizId);

    List<QuizAttempt> findByQuizIdOrderByFinishedAtDesc(Long quizId);

    long countByStudentIdAndQuizId(Long studentId, Long quizId);

    List<QuizAttempt> findByQuizCourseIdOrderByFinishedAtDesc(Long courseId);
}
