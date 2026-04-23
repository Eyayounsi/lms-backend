package com.elearning.ProjetPfe.repository.learning;

import com.elearning.ProjetPfe.entity.course.Lesson;
import com.elearning.ProjetPfe.entity.course.Course;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.learning.Quiz;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, Long> {

    List<Quiz> findByCourseIdOrderByCreatedAtDesc(Long courseId);

    List<Quiz> findByCourseInstructorIdOrderByCreatedAtDesc(Long instructorId);

    /** Quizzes liés à une leçon spécifique */
    List<Quiz> findByLessonId(Long lessonId);

    /** Vérifier si une leçon a un quiz */
    boolean existsByLessonId(Long lessonId);

    /** Trouver le premier quiz d'une leçon */
    java.util.Optional<Quiz> findFirstByLessonId(Long lessonId);

    /** Quizzes de l'instructor (par lesson ou par course) */
    List<Quiz> findByLessonSectionCourseInstructorIdOrderByCreatedAtDesc(Long instructorId);

    /**
     * Vérifie si un étudiant a passé un quiz avec succès.
     * Un quiz est considéré comme réussi si l'étudiant a au moins une tentative
     * avec un score >= au score minimum requis (passMark).
     */
    @org.springframework.data.jpa.repository.Query("""
        SELECT CASE WHEN COUNT(qa) > 0 THEN true ELSE false END
        FROM QuizAttempt qa
        WHERE qa.student.id = :studentId
        AND qa.quiz.id = :quizId
        AND qa.score >= qa.quiz.passMark
        """)
    boolean hasStudentPassedQuiz(Long studentId, Long quizId);
}
