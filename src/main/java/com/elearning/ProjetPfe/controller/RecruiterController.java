package com.elearning.ProjetPfe.controller;

import com.elearning.ProjetPfe.entity.Role;
import com.elearning.ProjetPfe.entity.User;
import com.elearning.ProjetPfe.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Endpoints RECRUITER — accessibles uniquement avec le rôle RECRUITER.
 *
 * GET  /api/recruiter/stats            → statistiques du tableau de bord
 * GET  /api/recruiter/shared-profiles  → profils étudiants partagés avec les recruteurs
 */
@RestController
@RequestMapping("/api/recruiter")
public class RecruiterController {

    private final UserRepository userRepository;

    public RecruiterController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // ─── Statistiques tableau de bord ────────────────────────────────────────

    /**
     * Retourne des statistiques consolidées :
     * - sharedProfiles  : nombre d'étudiants qui partagent leur profil
     * - totalStudents   : nombre total d'étudiants inscrits
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @AuthenticationPrincipal User currentUser) {

        long sharedProfiles = userRepository
                .findByRoleAndShareWithRecruiters(Role.STUDENT, true)
                .size();
        long totalStudents = userRepository.countByRole(Role.STUDENT);

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("sharedProfiles", sharedProfiles);
        stats.put("totalStudents", totalStudents);
        // Valeurs calculées côté front ou stub pour l'instant
        stats.put("pendingOffers", 0);
        stats.put("scheduledInterviews", 0);

        return ResponseEntity.ok(stats);
    }

    // ─── Profils étudiants partagés ──────────────────────────────────────────

    /**
     * Retourne la liste des étudiants qui ont activé "partager avec les recruteurs".
     * Seules les informations publiques sont incluses.
     */
    @GetMapping("/shared-profiles")
    public ResponseEntity<List<Map<String, Object>>> getSharedProfiles(
            @AuthenticationPrincipal User currentUser) {

        List<User> students = userRepository
                .findByRoleAndShareWithRecruiters(Role.STUDENT, true);

        List<Map<String, Object>> profiles = new ArrayList<>();
        for (User s : students) {
            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("id", s.getId());
            profile.put("fullName", s.getFullName());
            profile.put("email", s.getEmail());
            profile.put("avatarPath", s.getAvatarPath());
            profile.put("bio", s.getBio());
            profile.put("designation", s.getDesignation());
            profile.put("linkedinUrl", s.getLinkedinUrl());
            profile.put("educationJson", s.getEducationJson());
            profile.put("experienceJson", s.getExperienceJson());
            profiles.add(profile);
        }

        return ResponseEntity.ok(profiles);
    }
}
