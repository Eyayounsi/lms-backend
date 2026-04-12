package com.elearning.ProjetPfe.controller.analytics;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.dto.engagement.AdminAnalyticsDto;
import com.elearning.ProjetPfe.dto.engagement.CourseAnalyticsDto;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.auth.UserRepository;
import com.elearning.ProjetPfe.service.engagement.AnalyticsService;

/**
 * Endpoints analytics pour Instructor et Admin.
 *
 *   GET /api/instructor/analytics  → List<CourseAnalyticsDto>
 *   GET /api/admin/analytics       → AdminAnalyticsDto
 */
@RestController
@RequestMapping("/api")
public class AnalyticsController {

    @Autowired private AnalyticsService analyticsService;
    @Autowired private UserRepository userRepository;

    private User resolveUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
    }

    /**
     * GET /api/instructor/analytics
     * Retourne les analytics pédagogiques de tous les cours publiés de l'instructor.
     */
    @GetMapping("/instructor/analytics")
    public ResponseEntity<List<CourseAnalyticsDto>> getInstructorAnalytics(Authentication authentication) {
        User instructor = resolveUser(authentication);
        return ResponseEntity.ok(analyticsService.getInstructorAnalytics(instructor));
    }

    /**
     * GET /api/admin/analytics
     * Retourne les analytics globaux de la plateforme (tous cours publiés).
     */
    @GetMapping("/admin/analytics")
    public ResponseEntity<AdminAnalyticsDto> getAdminAnalytics() {
        return ResponseEntity.ok(analyticsService.getAdminAnalytics());
    }
}
