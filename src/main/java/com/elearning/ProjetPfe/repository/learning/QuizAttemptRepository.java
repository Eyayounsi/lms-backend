package com.elearning.ProjetPfe.repository.learning;

import com.elearning.ProjetPfe.entity.learning.Quiz;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.learning.QuizAttempt;

@Repository
public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {

    List<QuizAttempt> findByStudentIdAndQuizIdOrderByStartedAtDesc(Long studentId, Long quizId);

    List<QuizAttempt> findByQuizIdOrderByFinishedAtDesc(Long quizId);

    long countByStudentIdAndQuizId(Long studentId, Long quizId);

    List<QuizAttempt> findByQuizCourseIdOrderByFinishedAtDesc(Long courseId);

    /** Nombre de quiz distincts réussis par un étudiant */
    @Query("SELECT COUNT(DISTINCT qa.quiz.id) FROM QuizAttempt qa WHERE qa.student.id = :studentId AND qa.passed = true")
    long countDistinctPassedByStudentId(@Param("studentId") Long studentId);
}
