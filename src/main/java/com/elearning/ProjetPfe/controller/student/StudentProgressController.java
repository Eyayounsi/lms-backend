package com.elearning.ProjetPfe.controller.student;

import com.elearning.ProjetPfe.entity.course.Lesson;
import com.elearning.ProjetPfe.entity.course.Course;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.dto.learning.CourseProgressDto;
import com.elearning.ProjetPfe.dto.learning.LessonProgressDto;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.auth.UserRepository;
import com.elearning.ProjetPfe.service.learning.ProgressService;

/**
 * Endpoints pour la progression des étudiants dans les cours.
 *
 * ═══════════════════════════════════════════════════
 * 1. SAUVEGARDE AUTOMATIQUE (vidéo player)
 * ═══════════════════════════════════════════════════
 *
 *   Le frontend appelle toutes les 30 secondes pendant la lecture :
 *
 *   PUT /api/student/progress/lesson/{lessonId}
 *   Body: { "watchedSeconds": 275 }
 *
 *   → Sauvegarde la position
 *   → Renvoie si la leçon vient d'être complétée (>= 80%)
 *
 * ═══════════════════════════════════════════════════
 * 2. CHARGEMENT (quand l'étudiant ouvre une leçon)
 * ═══════════════════════════════════════════════════
 *
 *   GET /api/student/progress/lesson/{lessonId}
 *   → Retourne { watchedSeconds: 245, completed: false }
 *   → Frontend : videoPlayer.currentTime = 245
 *
 * ═══════════════════════════════════════════════════
 * 3. PROGRESSION DU COURS
 * ═══════════════════════════════════════════════════
 *
 *   GET /api/student/progress/course/{courseId}
 *   → Retourne le % d'avancement + lastLessonId
 *
 *   GET /api/student/progress/my-courses
 *   → Tous les cours avec leur progression (pour "Mes cours")
 *
 * ═══════════════════════════════════════════════════
 * EXEMPLE POSTMAN (sauvegarde position vidéo) :
 * ═══════════════════════════════════════════════════
 *   PUT http://localhost:8081/api/student/progress/lesson/3
 *   Authorization: Bearer <token étudiant>
 *   Content-Type: application/json
 *   Body: { "watchedSeconds": 450 }
 *
 *   Réponse si la leçon vient d'être complétée (>= 80%) :
 *   { "lessonId": 3, "watchedSeconds": 450, "completed": true, "completedAt": "..." }
 */
@RestController
@RequestMapping("/api/student/progress")
public class StudentProgressController {

    @Autowired
    private ProgressService progressService;

    @Autowired
    private UserRepository userRepository;

    /** Résout l'utilisateur courant à partir du token JWT (même pattern que StudentCourseController). */
    private User resolveStudent(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    // ─── LEÇON — Obtenir la position sauvegardée ─────────────────────────

    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<LessonProgressDto> getLessonProgress(
            @PathVariable Long lessonId,
            Authentication authentication) {
        User student = resolveStudent(authentication);
        return ResponseEntity.ok(progressService.getLessonProgress(lessonId, student));
    }

    // ─── LEÇON — Sauvegarder la position (appelé toutes les 30s) ─────────

    @PutMapping("/lesson/{lessonId}")
    public ResponseEntity<LessonProgressDto> saveProgress(
            @PathVariable Long lessonId,
            @RequestBody Map<String, Long> body,
            Authentication authentication) {
        User student = resolveStudent(authentication);
        Long watchedSeconds = body.get("watchedSeconds");
        if (watchedSeconds == null || watchedSeconds < 0) {
            watchedSeconds = 0L;
        }
        return ResponseEntity.ok(progressService.saveProgress(lessonId, watchedSeconds, student));
    }

    // ─── LEÇON TEXT/PDF — Marquer comme terminée manuellement ────────────

    @PostMapping("/lesson/{lessonId}/complete")
    public ResponseEntity<LessonProgressDto> markCompleted(
            @PathVariable Long lessonId,
            Authentication authentication) {
        User student = resolveStudent(authentication);
        return ResponseEntity.ok(progressService.markLessonCompleted(lessonId, student));
    }

    // ─── LEÇON — Démarquer comme non terminée (toggle) ───────────────────

    @PostMapping("/lesson/{lessonId}/incomplete")
    public ResponseEntity<LessonProgressDto> markIncomplete(
            @PathVariable Long lessonId,
            Authentication authentication) {
        User student = resolveStudent(authentication);
        return ResponseEntity.ok(progressService.markLessonIncomplete(lessonId, student));
    }

    // ─── COURS — Progression globale ─────────────────────────────────────

    @GetMapping("/course/{courseId}")
    public ResponseEntity<CourseProgressDto> getCourseProgress(
            @PathVariable Long courseId,
            Authentication authentication) {
        User student = resolveStudent(authentication);
        return ResponseEntity.ok(progressService.getCourseProgress(courseId, student));
    }

    // ─── MES COURS — Tous les cours avec progression ─────────────────────

    @GetMapping("/my-courses")
    public ResponseEntity<List<CourseProgressDto>> getMyCourseProgress(
            Authentication authentication) {
        User student = resolveStudent(authentication);
        return ResponseEntity.ok(progressService.getMyCourseProgress(student));
    }
}
