package com.elearning.ProjetPfe.controller.course;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.entity.course.Course;
import com.elearning.ProjetPfe.entity.course.CourseStatus;
import com.elearning.ProjetPfe.entity.payment.PaymentStatus;
import com.elearning.ProjetPfe.entity.auth.Role;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.course.CourseRepository;
import com.elearning.ProjetPfe.repository.payment.EnrollmentRepository;
import com.elearning.ProjetPfe.repository.engagement.ReviewRepository;
import com.elearning.ProjetPfe.repository.auth.UserRepository;

/**
 * Endpoints publics généraux (accessibles sans token).
 *
 * Utilise des chemins absolus (pas de @RequestMapping au niveau classe)
 * pour éviter la concaténation involontaire de préfixes.
 *
 * GET /api/public/instructor/{instructorId}  → profil public d'un instructeur
 */
@RestController
public class PublicController {

    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private ReviewRepository reviewRepository;

    // ─── PROFIL PUBLIC D'UN INSTRUCTEUR ──────────────────────────────────
    // GET /api/public/instructor/{instructorId} — accessible SANS connexion
    @GetMapping("/api/public/instructor/{instructorId}")
    public ResponseEntity<Map<String, Object>> getPublicInstructorProfile(
            @PathVariable Long instructorId) {

        User instructor = userRepository.findById(instructorId)
                .orElseThrow(() -> new RuntimeException("Instructeur introuvable"));

        if (instructor.getRole() != Role.INSTRUCTOR) {
            throw new RuntimeException("Cet utilisateur n'est pas un instructeur");
        }

        // Cours publiés
        List<Course> courses = courseRepository.findByInstructorIdAndStatus(
                instructorId, CourseStatus.PUBLISHED);

        List<Map<String, Object>> courseDtos = courses.stream().map(c -> {
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("id", c.getId());
            cm.put("title", c.getTitle());
            cm.put("thumbnail", c.getCoverImage());
            cm.put("price", c.getPrice());
            cm.put("level", c.getLevel() != null ? c.getLevel().name() : null);
            cm.put("categoryName", c.getCategory() != null ? c.getCategory().getName() : null);
            long studentCount = enrollmentRepository
                    .countByCourseIdAndPaymentStatus(c.getId(), PaymentStatus.PAID);
            cm.put("studentCount", studentCount);
            long lessonCount = c.getSections().stream()
                    .mapToLong(s -> s.getLessons().size()).sum();
            cm.put("lessonCount", lessonCount);
            return cm;
        }).collect(Collectors.toList());

        // Statistiques globales
        long totalStudents = courses.stream()
                .mapToLong(c -> enrollmentRepository
                        .countByCourseIdAndPaymentStatus(c.getId(), PaymentStatus.PAID))
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
