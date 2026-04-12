package com.elearning.ProjetPfe.controller.course;

import com.elearning.ProjetPfe.entity.course.Course;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.dto.learning.CertificateDto;
import com.elearning.ProjetPfe.dto.course.CourseResponseDto;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.auth.UserRepository;
import com.elearning.ProjetPfe.service.learning.CertificateService;
import com.elearning.ProjetPfe.service.course.CourseService;

/**
 * Endpoints publics et étudiants pour les cours.
 *
 * ─── PUBLIC (sans token) ──────────────────────────────────────────────────
 *
 *   GET /api/courses
 *     → Tous les cours publiés
 *     → Params optionnels :
 *         ?search=python           → Filtre par mot-clé dans titre+description
 *         ?categoryId=3            → Filtre par catégorie
 *         ?level=BEGINNER          → Filtre par niveau
 *
 *   GET /api/courses/{id}
 *     → Détail d'un cours publié (avec curriculum public)
 *
 * ─── STUDENT (avec token) ─────────────────────────────────────────────────
 *
 *   GET /api/courses/{id}/content
 *     → Contenu complet avec les URLs de vidéo (seulement si payé)
 */
@RestController
@RequestMapping("/api/courses")
public class CoursePublicController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CertificateService certificateService;

    @Autowired
    private UserRepository userRepository;

    // ─── LISTE des cours publiés ──────────────────────────────────────────

    @GetMapping
    public ResponseEntity<List<CourseResponseDto>> getPublishedCourses(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String level) {
        return ResponseEntity.ok(
                courseService.getPublishedCourses(search, categoryId, level)
        );
    }

    // ─── COURS MIS EN AVANT (featured) ───────────────────────────────────

    @GetMapping("/featured")
    public ResponseEntity<List<CourseResponseDto>> getFeaturedCourses() {
        return ResponseEntity.ok(courseService.getFeaturedCourses());
    }

    // ─── DÉTAIL d'un cours (curriculum public : leçons sans URL si payantes) ─

    @GetMapping("/{courseId}")
    public ResponseEntity<CourseResponseDto> getCourseDetail(
            @PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getPublicCourseDetail(courseId));
    }

    // ─── CONTENU COMPLET (URLs vidéo — seulement si payé) ────────────────

    @GetMapping("/{courseId}/content")
    public ResponseEntity<CourseResponseDto> getCourseContent(
            @PathVariable Long courseId,
            Authentication authentication) {
        String email = authentication.getName();
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return ResponseEntity.ok(courseService.getCourseContent(courseId, student));
    }

    // ─── VÉRIFICATION PUBLIQUE d'un certificat par son code unique ───────
    // GET /api/courses/certificates/verify/{code}
    // Accessible SANS token — pour que n'importe qui puisse vérifier

    @GetMapping("/certificates/verify/{code}")
    public ResponseEntity<CertificateDto> verifyCertificate(
            @PathVariable String code) {
        return ResponseEntity.ok(certificateService.verifyByCode(code));
    }
}
