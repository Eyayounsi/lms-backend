package com.elearning.ProjetPfe.controller.auth;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.elearning.ProjetPfe.dto.auth.ChangePasswordDto;
import com.elearning.ProjetPfe.dto.auth.UpdateProfileDto;
import com.elearning.ProjetPfe.dto.auth.UserProfileDto;
import com.elearning.ProjetPfe.entity.auth.Role;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.entity.course.Course;
import com.elearning.ProjetPfe.entity.course.CourseStatus;
import com.elearning.ProjetPfe.entity.payment.PaymentStatus;
import com.elearning.ProjetPfe.repository.auth.UserRepository;
import com.elearning.ProjetPfe.repository.course.CourseRepository;
import com.elearning.ProjetPfe.repository.engagement.ReviewRepository;
import com.elearning.ProjetPfe.repository.payment.EnrollmentRepository;
import com.elearning.ProjetPfe.service.auth.UserService;
import com.elearning.ProjetPfe.service.course.CourseService;
import com.elearning.ProjetPfe.service.course.FileStorageService;

import jakarta.validation.Valid;

/**
 * Controller pour la gestion de profil des utilisateurs connectés.
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private FileStorageService fileStorageService;
    @Autowired private CourseService courseService;
    @Autowired private CourseRepository courseRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private ReviewRepository reviewRepository;

    // ─── GET PROFIL ───────────────────────────────────────────────────────────
    // GET /api/user/profile
    // Retourne les données de l'utilisateur actuellement connecté
    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getProfile(
            @AuthenticationPrincipal User currentUser) {

        UserProfileDto profile = userService.getProfile(currentUser.getEmail());
        return ResponseEntity.ok(profile);
    }

    // ─── UPDATE PROFIL ────────────────────────────────────────────────────────
    // PUT /api/user/profile
    // Modifier nom, téléphone, email
    @PutMapping("/profile")
    public ResponseEntity<UserProfileDto> updateProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateProfileDto request) {

        UserProfileDto updated = userService.updateProfile(currentUser.getEmail(), request);
        return ResponseEntity.ok(updated);
    }

    // ─── CHANGER MOT DE PASSE ─────────────────────────────────────────────────
    // PUT /api/user/change-password
    // Exige l'ancien mot de passe pour changer le nouveau
    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ChangePasswordDto request) {

        userService.changePassword(currentUser.getEmail(), request);
        return ResponseEntity.ok("Mot de passe modifié avec succès");
    }

    // ─── PARTAGE DE PROFIL AVEC LES RECRUTEURS ─────────────────────────────────────
    // PUT /api/user/share-profile
    @PutMapping("/share-profile")
    public ResponseEntity<Map<String, Object>> toggleShareProfile(
            @AuthenticationPrincipal User currentUser,
            @RequestBody Map<String, Boolean> body) {
        boolean share = Boolean.TRUE.equals(body.get("share"));
        User user = currentUser;
        user.setShareWithRecruiters(share);
        // Persister via le service réutilisé ou directement via le repository injecté
        // Utiliser userService pour garder les évènements @PreUpdate
        userService.updateShareProfile(currentUser.getEmail(), share);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("shareWithRecruiters", share);
        resp.put("message", share ? "Profil partagé avec les recruteurs" : "Partage désactivé");
        return ResponseEntity.ok(resp);
    }

    // ─── PING / VÉRIFICATION STATUT ──────────────────────────────────────────
    // GET /api/user/ping
    // Utilisé par le guard Angular pour vérifier si le compte est encore actif.
    // Si le compte est BLOQUÉ, JwtFilter retourne 423 avant d'atteindre ce code.
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("ok");
    }

    // ─── SUPPRIMER COMPTE ─────────────────────────────────────────────────────
    // DELETE /api/user/account?password=xxx
    // Exige le mot de passe comme confirmation avant suppression définitive
    @DeleteMapping("/account")
    public ResponseEntity<String> deleteAccount(
            @AuthenticationPrincipal User currentUser,
            @RequestParam String password) {

        userService.deleteAccount(currentUser.getEmail(), password);
        return ResponseEntity.ok("Compte supprimé avec succès");
    }

    // ─── AVATAR UPLOAD ────────────────────────────────────────────────────────
    // POST /api/user/avatar
    @PostMapping("/avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            Authentication auth) {
        MultipartFile effectiveFile = Optional.ofNullable(file).orElse(avatar);
        if (effectiveFile == null || effectiveFile.isEmpty()) {
            throw new RuntimeException("Aucun fichier image reçu. Vérifiez le champ multipart 'file'.");
        }

        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        String path = fileStorageService.storeFile(effectiveFile, "avatars");
        user.setAvatarPath(path);
        userRepository.save(user);

        Map<String, String> result = new LinkedHashMap<>();
        result.put("avatarPath", path);
        return ResponseEntity.ok(result);
    }

    // ─── AVATAR DELETE ────────────────────────────────────────────────────────
    // DELETE /api/user/avatar
    @DeleteMapping("/avatar")
    public ResponseEntity<Map<String, String>> deleteAvatar(Authentication auth) {
        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        String oldPath = user.getAvatarPath();
        if (oldPath != null && !oldPath.isBlank()) {
            fileStorageService.deleteFile(oldPath);
        }

        user.setAvatarPath(null);
        userRepository.save(user);

        Map<String, String> result = new LinkedHashMap<>();
        result.put("message", "Photo de profil supprimée avec succès");
        result.put("avatarPath", "");
        return ResponseEntity.ok(result);
    }

    // ─── PROFIL PUBLIC INSTRUCTOR ─────────────────────────────────────────────
    // GET /api/user/instructor/{instructorId}/profile — accessible par tout authentifié
    @GetMapping("/instructor/{instructorId}/profile")
    public ResponseEntity<Map<String, Object>> getInstructorPublicProfile(
            @PathVariable Long instructorId) {
        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new RuntimeException("Instructeur introuvable"));

        if (instructor.getRole() != Role.INSTRUCTOR) {
            throw new RuntimeException("Cet utilisateur n'est pas un instructeur");
        }

        // Cours publiés
        List<Course> courses = courseRepository.findByInstructorIdAndStatus(instructorId, CourseStatus.PUBLISHED);
        List<Map<String, Object>> courseDtos = courses.stream().map(c -> {
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("id", c.getId());
            cm.put("title", c.getTitle());
            cm.put("thumbnail", c.getCoverImage());
            cm.put("price", c.getPrice());
            cm.put("level", c.getLevel() != null ? c.getLevel().name() : null);
            cm.put("categoryName", c.getCategory() != null ? c.getCategory().getName() : null);
            // Compter les étudiants inscrits
            long studentCount = enrollmentRepository.countByCourseIdAndPaymentStatus(c.getId(), PaymentStatus.PAID);
            cm.put("studentCount", studentCount);
            // Compter les leçons
            long lessonCount = c.getSections().stream().mapToLong(s -> s.getLessons().size()).sum();
            cm.put("lessonCount", lessonCount);
            return cm;
        }).collect(Collectors.toList());

        // Statistiques globales
        long totalStudents = courses.stream()
                .mapToLong(c -> enrollmentRepository.countByCourseIdAndPaymentStatus(c.getId(), PaymentStatus.PAID))
                .sum();
        long totalLessons = courses.stream()
                .flatMap(c -> c.getSections().stream())
                .mapToLong(s -> s.getLessons().size())
                .sum();
        long totalReviews = courses.stream()
                .mapToLong(c -> reviewRepository.countByCourseId(c.getId()))
                .sum();
        double avgRating = courses.stream()
                .map(c -> reviewRepository.calculateAverageRating(c.getId()))
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average().orElse(0.0);

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", instructor.getId());
        profile.put("fullName", instructor.getFullName());
        profile.put("email", instructor.getEmail());
        profile.put("phone", instructor.getPhone());
        profile.put("avatarPath", instructor.getAvatarPath());
        profile.put("bio", instructor.getBio());
        profile.put("aboutMe", instructor.getAboutMe());
        profile.put("designation", instructor.getDesignation());
        profile.put("address", instructor.getAddress());
        profile.put("facebookUrl", instructor.getFacebookUrl());
        profile.put("instagramUrl", instructor.getInstagramUrl());
        profile.put("twitterUrl", instructor.getTwitterUrl());
        profile.put("youtubeUrl", instructor.getYoutubeUrl());
        profile.put("linkedinUrl", instructor.getLinkedinUrl());
        profile.put("educationJson", instructor.getEducationJson());
        profile.put("experienceJson", instructor.getExperienceJson());
        profile.put("totalCourses", courses.size());
        profile.put("totalStudents", totalStudents);
        profile.put("totalLessons", totalLessons);
        profile.put("totalReviews", totalReviews);
        profile.put("averageRating", Math.round(avgRating * 10.0) / 10.0);
        profile.put("courses", courseDtos);
        return ResponseEntity.ok(profile);
    }
}
