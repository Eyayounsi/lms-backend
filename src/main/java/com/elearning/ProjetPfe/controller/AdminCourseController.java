package com.elearning.ProjetPfe.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.dto.CourseResponseDto;
import com.elearning.ProjetPfe.dto.CourseReviewDto;
import com.elearning.ProjetPfe.dto.CreateCourseDto;
import com.elearning.ProjetPfe.service.CourseService;

/**
 * Endpoints admin pour la validation des cours.
 *
 * URL : /api/admin/courses/...
 * Sécurisé : hasAuthority("ADMIN") dans SecurityConfig
 */
@RestController
@RequestMapping("/api/admin/courses")
public class AdminCourseController {

    @Autowired
    private CourseService courseService;

    // ─── VOIR les cours en attente de validation ─────────────────────────
    // GET /api/admin/courses/pending
    @GetMapping("/pending")
    public ResponseEntity<List<CourseResponseDto>> getPendingCourses() {
        return ResponseEntity.ok(courseService.getPendingCourses());
    }

    // ─── VOIR les cours publiés ───────────────────────────────────────────
    // GET /api/admin/courses/published
    @GetMapping("/published")
    public ResponseEntity<List<CourseResponseDto>> getPublishedCourses() {
        return ResponseEntity.ok(courseService.getPublishedCoursesForAdmin());
    }

    // ─── VOIR tous les cours (tous statuts) ──────────────────────────────
    // GET /api/admin/courses/all
    @GetMapping("/all")
    public ResponseEntity<List<CourseResponseDto>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCoursesForAdmin());
    }

    // ─── VOIR un cours en détail ─────────────────────────────────────────
    // GET /api/admin/courses/5
    @GetMapping("/{courseId}")
    public ResponseEntity<CourseResponseDto> getCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getCourseById(courseId));
    }

    // ─── ACCEPTER ou REJETER un cours ────────────────────────────────────
    @PutMapping("/{courseId}/review")
    public ResponseEntity<CourseResponseDto> reviewCourse(
            @PathVariable Long courseId,
            @RequestBody CourseReviewDto dto) {
        return ResponseEntity.ok(courseService.reviewCourse(courseId, dto));
    }

    // ─── SUPPRIMER un cours (admin) ───────────────────────────────────────
    @DeleteMapping("/{courseId}")
    public ResponseEntity<String> deleteCourse(@PathVariable Long courseId) {
        try {
            courseService.adminDeleteCourse(courseId);
            return ResponseEntity.ok("Cours supprimé");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ─── ARCHIVER un cours (admin) ────────────────────────────────────────
    @PutMapping("/{courseId}/archive")
    public ResponseEntity<?> archiveCourse(@PathVariable Long courseId) {
        try {
            return ResponseEntity.ok(courseService.adminArchiveCourse(courseId));
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("Data truncated")) {
                return ResponseEntity.badRequest().body(java.util.Map.of(
                    "error", "La colonne 'status' en base de données doit être de type VARCHAR(20). "
                           + "Exécutez : ALTER TABLE courses MODIFY COLUMN status VARCHAR(20) NOT NULL;"
                ));
            }
            return ResponseEntity.badRequest().body(java.util.Map.of("error", msg != null ? msg : "Erreur lors de l'archivage"));
        }
    }

    // ─── MODIFIER LE CONTENU D'UN COURS (admin) ─────────────────────────
    // PUT /api/admin/courses/5/edit
    @PutMapping("/{courseId}/edit")
    public ResponseEntity<CourseResponseDto> adminEditCourse(
            @PathVariable Long courseId,
            @RequestBody CreateCourseDto dto) {
        return ResponseEntity.ok(courseService.adminEditCourse(courseId, dto));
    }

    // ─── MODIFIER LE PRIX (admin) ─────────────────────────────────────────
    // PUT /api/admin/courses/5/price  Body: { "price": 29.99 }
    @PutMapping("/{courseId}/price")
    public ResponseEntity<CourseResponseDto> updatePrice(
            @PathVariable Long courseId,
            @RequestBody java.util.Map<String, java.math.BigDecimal> body) {
        return ResponseEntity.ok(courseService.adminUpdatePrice(courseId, body.get("price")));
    }

    // ─── METTRE EN PROMOTION (admin) ──────────────────────────────────────
    // PUT /api/admin/courses/5/promotion  Body: { "discountPrice": 9.99, "discountEndsAt": "2026-04-01T00:00:00" }
    @PutMapping("/{courseId}/promotion")
    public ResponseEntity<CourseResponseDto> setPromotion(
            @PathVariable Long courseId,
            @RequestBody java.util.Map<String, Object> body) {
        java.math.BigDecimal dp = body.get("discountPrice") != null
                ? new java.math.BigDecimal(body.get("discountPrice").toString()) : null;
        java.time.LocalDateTime endsAt = body.get("discountEndsAt") != null
                ? java.time.LocalDateTime.parse(body.get("discountEndsAt").toString()) : null;
        return ResponseEntity.ok(courseService.adminSetPromotion(courseId, dp, endsAt));
    }

    // ─── SUPPRIMER UN COMMENTAIRE (admin) ──────────────────────────────────
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<String> deleteReview(
            @PathVariable Long reviewId,
            @Autowired com.elearning.ProjetPfe.repository.ReviewRepository reviewRepository) {
        reviewRepository.deleteById(reviewId);
        return ResponseEntity.ok("Avis supprimé");
    }

    // ─── FEATURED TOGGLE ─────────────────────────────────────────────────
    // PUT /api/admin/courses/{courseId}/featured  Body: { "featured": true }
    @PutMapping("/{courseId}/featured")
    public ResponseEntity<CourseResponseDto> toggleFeatured(
            @PathVariable Long courseId,
            @RequestBody java.util.Map<String, Boolean> body) {
        boolean featured = Boolean.TRUE.equals(body.get("featured"));
        return ResponseEntity.ok(courseService.toggleFeatured(courseId, featured));
    }

    // ─── COURS AVEC MODIFICATIONS EN ATTENTE ─────────────────────────────
    // GET /api/admin/courses/pending-edits
    @GetMapping("/pending-edits")
    public ResponseEntity<List<CourseResponseDto>> getCoursesWithPendingEdits() {
        return ResponseEntity.ok(courseService.getCoursesWithPendingEdits());
    }

    // ─── APPROUVER UNE MODIFICATION EN ATTENTE ───────────────────────────
    // PUT /api/admin/courses/{courseId}/approve-edit
    @PutMapping("/{courseId}/approve-edit")
    public ResponseEntity<CourseResponseDto> approvePendingEdit(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.approvePendingEdit(courseId));
    }

    // ─── REJETER UNE MODIFICATION EN ATTENTE ─────────────────────────────
    // PUT /api/admin/courses/{courseId}/reject-edit  Body: { "reason": "..." }
    @PutMapping("/{courseId}/reject-edit")
    public ResponseEntity<CourseResponseDto> rejectPendingEdit(
            @PathVariable Long courseId,
            @RequestBody java.util.Map<String, String> body) {
        String reason = body.get("reason");
        return ResponseEntity.ok(courseService.rejectPendingEdit(courseId, reason));
    }
}
