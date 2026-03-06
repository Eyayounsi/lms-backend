package com.elearning.ProjetPfe.controller;

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

import com.elearning.ProjetPfe.entity.User;
import com.elearning.ProjetPfe.repository.UserRepository;
import com.elearning.ProjetPfe.service.QaService;

/**
 * Endpoints Q&A — accessible par tout utilisateur authentifié.
 * Les étudiants posent des questions, les instructeurs répondent.
 */
@RestController
@RequestMapping("/api/user/qa")
public class QaController {

    @Autowired private QaService qaService;
    @Autowired private UserRepository userRepository;

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    // ─── Questions ───────────────────────────────────────────────────────

    /** POST /api/user/qa/questions — Poser une question */
    @PostMapping("/questions")
    public ResponseEntity<Map<String, Object>> askQuestion(
            @RequestBody Map<String, Object> data, Authentication auth) {
        return ResponseEntity.ok(qaService.askQuestion(data, getUser(auth)));
    }

    /** GET /api/user/qa/course/{courseId} — Questions d'un cours */
    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<Map<String, Object>>> getQuestionsByCourse(
            @PathVariable Long courseId) {
        return ResponseEntity.ok(qaService.getQuestionsByCourse(courseId));
    }

    /** GET /api/user/qa/lesson/{lessonId} — Questions d'une leçon */
    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<List<Map<String, Object>>> getQuestionsByLesson(
            @PathVariable Long lessonId) {
        return ResponseEntity.ok(qaService.getQuestionsByLesson(lessonId));
    }

    /** GET /api/user/qa/questions/{questionId} — Détail d'une question */
    @GetMapping("/questions/{questionId}")
    public ResponseEntity<Map<String, Object>> getQuestionDetail(
            @PathVariable Long questionId) {
        return ResponseEntity.ok(qaService.getQuestionDetail(questionId));
    }

    /** DELETE /api/user/qa/questions/{questionId} — Supprimer sa question */
    @DeleteMapping("/questions/{questionId}")
    public ResponseEntity<String> deleteQuestion(
            @PathVariable Long questionId, Authentication auth) {
        qaService.deleteQuestion(questionId, getUser(auth));
        return ResponseEntity.ok("Question supprimée");
    }

    /** PUT /api/user/qa/questions/{questionId} — Modifier sa question */
    @PutMapping("/questions/{questionId}")
    public ResponseEntity<Map<String, Object>> updateQuestion(
            @PathVariable Long questionId,
            @RequestBody Map<String, Object> data, Authentication auth) {
        return ResponseEntity.ok(qaService.updateQuestion(questionId, data, getUser(auth)));
    }

    // ─── Réponses ────────────────────────────────────────────────────────

    /** POST /api/user/qa/questions/{questionId}/answers — Répondre */
    @PostMapping("/questions/{questionId}/answers")
    public ResponseEntity<Map<String, Object>> answerQuestion(
            @PathVariable Long questionId,
            @RequestBody Map<String, Object> data, Authentication auth) {
        return ResponseEntity.ok(qaService.answerQuestion(questionId, data, getUser(auth)));
    }

    /** DELETE /api/user/qa/answers/{answerId} — Supprimer sa réponse */
    @DeleteMapping("/answers/{answerId}")
    public ResponseEntity<String> deleteAnswer(
            @PathVariable Long answerId, Authentication auth) {
        qaService.deleteAnswer(answerId, getUser(auth));
        return ResponseEntity.ok("Réponse supprimée");
    }

    /** PUT /api/user/qa/answers/{answerId} — Modifier sa réponse */
    @PutMapping("/answers/{answerId}")
    public ResponseEntity<Map<String, Object>> updateAnswer(
            @PathVariable Long answerId,
            @RequestBody Map<String, Object> data, Authentication auth) {
        return ResponseEntity.ok(qaService.updateAnswer(answerId, data, getUser(auth)));
    }
}
