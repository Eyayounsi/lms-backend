package com.elearning.ProjetPfe.controller.instructor;

import com.elearning.ProjetPfe.entity.learning.Question;
import com.elearning.ProjetPfe.entity.learning.Quiz;
import com.elearning.ProjetPfe.entity.course.Lesson;
import com.elearning.ProjetPfe.entity.course.Course;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.auth.UserRepository;
import com.elearning.ProjetPfe.service.learning.QuizService;

/**
 * Endpoints instructor pour la gestion des quiz.
 * Sécurisé par SecurityConfig : hasAuthority("INSTRUCTOR")
 */
@RestController
@RequestMapping("/api/instructor/quiz")
public class InstructorQuizController {

    @Autowired private QuizService quizService;
    @Autowired private UserRepository userRepository;

    private User getInstructor(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    // ─── QUIZ CRUD ───────────────────────────────────────────────────────

    /** GET /api/instructor/quiz — Tous les quizzes de l'instructor */
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getMyQuizzes(Authentication auth) {
        return ResponseEntity.ok(quizService.getInstructorQuizzes(getInstructor(auth)));
    }

    /** GET /api/instructor/quiz/course/{courseId} — Quizzes d'un cours */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<Map<String, Object>>> getQuizzesByCourse(
            @PathVariable Long courseId, Authentication auth) {
        return ResponseEntity.ok(quizService.getQuizzesByCourse(courseId, getInstructor(auth)));
    }

    /** GET /api/instructor/quiz/lesson/{lessonId} — Quiz d'une leçon (null si aucun) */
    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<Map<String, Object>> getQuizByLesson(
            @PathVariable Long lessonId, Authentication auth) {
        Map<String, Object> result = quizService.getQuizByLesson(lessonId, getInstructor(auth));
        return result != null ? ResponseEntity.ok(result) : ResponseEntity.noContent().build();
    }

    /** POST /api/instructor/quiz — Créer un quiz */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createQuiz(
            @RequestBody Map<String, Object> data, Authentication auth) {
        return ResponseEntity.ok(quizService.createQuiz(data, getInstructor(auth)));
    }

    /** PUT /api/instructor/quiz/{quizId} — Modifier un quiz */
    @PutMapping("/{quizId}")
    public ResponseEntity<Map<String, Object>> updateQuiz(
            @PathVariable Long quizId,
            @RequestBody Map<String, Object> data, Authentication auth) {
        return ResponseEntity.ok(quizService.updateQuiz(quizId, data, getInstructor(auth)));
    }

    /** DELETE /api/instructor/quiz/{quizId} — Supprimer un quiz */
    @DeleteMapping("/{quizId}")
    public ResponseEntity<String> deleteQuiz(@PathVariable Long quizId, Authentication auth) {
        quizService.deleteQuiz(quizId, getInstructor(auth));
        return ResponseEntity.ok("Quiz supprimé");
    }

    // ─── QUESTIONS CRUD ──────────────────────────────────────────────────

    /** GET /api/instructor/quiz/{quizId}/questions — Lister les questions */
    @GetMapping("/{quizId}/questions")
    public ResponseEntity<List<Map<String, Object>>> getQuestions(
            @PathVariable Long quizId, Authentication auth) {
        return ResponseEntity.ok(quizService.getQuestions(quizId, getInstructor(auth)));
    }

    /** POST /api/instructor/quiz/{quizId}/questions — Ajouter une question */
    @PostMapping("/{quizId}/questions")
    public ResponseEntity<Map<String, Object>> addQuestion(
            @PathVariable Long quizId,
            @RequestBody Map<String, Object> data, Authentication auth) {
        return ResponseEntity.ok(quizService.addQuestion(quizId, data, getInstructor(auth)));
    }

    /** PUT /api/instructor/quiz/questions/{questionId} — Modifier une question */
    @PutMapping("/questions/{questionId}")
    public ResponseEntity<Map<String, Object>> updateQuestion(
            @PathVariable Long questionId,
            @RequestBody Map<String, Object> data, Authentication auth) {
        return ResponseEntity.ok(quizService.updateQuestion(questionId, data, getInstructor(auth)));
    }

    /** DELETE /api/instructor/quiz/questions/{questionId} — Supprimer une question */
    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<String> deleteQuestion(@PathVariable Long questionId, Authentication auth) {
        quizService.deleteQuestion(questionId, getInstructor(auth));
        return ResponseEntity.ok("Question supprimée");
    }

    // ─── RÉSULTATS ────────────────────────────────────────────────────────

    /** GET /api/instructor/quiz/{quizId}/results — Voir les résultats d'un quiz */
    @GetMapping("/{quizId}/results")
    public ResponseEntity<Map<String, Object>> getQuizResults(
            @PathVariable Long quizId, Authentication auth) {
        return ResponseEntity.ok(quizService.getQuizResults(quizId, getInstructor(auth)));
    }

    /** GET /api/instructor/quiz/attempt/{attemptId} — Détail d'une tentative */
    @GetMapping("/attempt/{attemptId}")
    public ResponseEntity<Map<String, Object>> getAttemptDetail(
            @PathVariable Long attemptId, Authentication auth) {
        return ResponseEntity.ok(quizService.getAttemptDetail(attemptId, getInstructor(auth)));
    }
}
