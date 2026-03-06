package com.elearning.ProjetPfe.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.dto.ReviewDto;
import com.elearning.ProjetPfe.entity.User;
import com.elearning.ProjetPfe.service.ReviewService;

/**
 * Endpoints pour les avis sur les cours.
 *
 * ─── PUBLIC (sans token) ───────────────────────────────────────────────────
 *   GET /api/public/courses/{courseId}/reviews
 *     → liste tous les avis du cours (pour la page publique du cours)
 *
 * ─── STUDENT (avec token) ─────────────────────────────────────────────────
 *   POST   /api/student/courses/{courseId}/reviews
 *     → créer ou modifier son avis (upsert)
 *     → Body: { "rating": 4, "comment": "Très bon cours !" }
 *
 *   DELETE /api/student/courses/{courseId}/reviews
 *     → supprimer son avis
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * EXEMPLE POSTMAN :
 * ═══════════════════════════════════════════════════════════════════════════
 *   POST http://localhost:8081/api/student/courses/1/reviews
 *   Authorization: Bearer <token étudiant ayant acheté le cours>
 *   Body: { "rating": 5, "comment": "Excellent ! Très bien expliqué." }
 */
@RestController
public class StudentReviewController {

    @Autowired
    private ReviewService reviewService;

    // ─── PUBLIC ───────────────────────────────────────────────────────────

    @GetMapping("/api/public/courses/{courseId}/reviews")
    public ResponseEntity<List<ReviewDto>> getCourseReviews(@PathVariable Long courseId) {
        return ResponseEntity.ok(reviewService.getCourseReviews(courseId));
    }

    // ─── STUDENT ──────────────────────────────────────────────────────────

    @PostMapping("/api/student/courses/{courseId}/reviews")
    public ResponseEntity<?> upsertReview(
            @PathVariable Long courseId,
            @RequestBody ReviewDto dto,
            @AuthenticationPrincipal User student) {
        try {
            return ResponseEntity.ok(reviewService.upsertReview(courseId, dto, student));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/api/student/courses/{courseId}/reviews")
    public ResponseEntity<String> deleteReview(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User student) {
        try {
            reviewService.deleteReview(courseId, student);
            return ResponseEntity.ok("Avis supprimé");
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}
