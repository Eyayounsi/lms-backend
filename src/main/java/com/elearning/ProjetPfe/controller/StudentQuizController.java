package com.elearning.ProjetPfe.controller;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.elearning.ProjetPfe.entity.User;
import com.elearning.ProjetPfe.repository.UserRepository;
import com.elearning.ProjetPfe.service.QuizService;

/**
 * Endpoints étudiant pour passer les quiz.
 * Sécurisé par SecurityConfig : anyRequest().authenticated()
 */
@RestController
@RequestMapping("/api/student/quiz")
public class StudentQuizController {

    @Autowired private QuizService quizService;
    @Autowired private UserRepository userRepository;

    private User getStudent(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    /** GET /api/student/quiz/course/{courseId} — Quizzes disponibles pour un cours */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<Map<String, Object>>> getStudentQuizzes(
            @PathVariable Long courseId, Authentication auth) {
        return ResponseEntity.ok(quizService.getStudentQuizzes(courseId, getStudent(auth)));
    }

    /** GET /api/student/quiz/all — Tous les quizzes de l'étudiant (toutes inscriptions) */
    @GetMapping("/all")
    public ResponseEntity<List<Map<String, Object>>> getAllStudentQuizzes(Authentication auth) {
        return ResponseEntity.ok(quizService.getAllStudentQuizzes(getStudent(auth)));
    }

    /** GET /api/student/quiz/{quizId}/start — Démarrer un quiz (questions sans réponses) */
    @GetMapping("/{quizId}/start")
    public ResponseEntity<Map<String, Object>> startQuiz(
            @PathVariable Long quizId, Authentication auth) {
        return ResponseEntity.ok(quizService.startQuiz(quizId, getStudent(auth)));
    }

    /** POST /api/student/quiz/{quizId}/submit — Soumettre les réponses */
    @PostMapping("/{quizId}/submit")
    public ResponseEntity<Map<String, Object>> submitQuiz(
            @PathVariable Long quizId,
            @RequestBody Map<String, Object> data, Authentication auth) {
        return ResponseEntity.ok(quizService.submitQuiz(quizId, data, getStudent(auth)));
    }

    /** GET /api/student/quiz/attempt/{attemptId} — Détail d'une tentative */
    @GetMapping("/attempt/{attemptId}")
    public ResponseEntity<Map<String, Object>> getAttemptDetail(
            @PathVariable Long attemptId, Authentication auth) {
        return ResponseEntity.ok(quizService.getAttemptDetail(attemptId, getStudent(auth)));
    }
}
