package com.elearning.ProjetPfe.service.engagement;

import com.elearning.ProjetPfe.entity.learning.Note;
import com.elearning.ProjetPfe.entity.learning.Quiz;
import com.elearning.ProjetPfe.entity.payment.Enrollment;
import com.elearning.ProjetPfe.entity.course.Section;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.elearning.ProjetPfe.dto.engagement.AdminAnalyticsDto;
import com.elearning.ProjetPfe.dto.engagement.CourseAnalyticsDto;
import com.elearning.ProjetPfe.entity.course.Course;
import com.elearning.ProjetPfe.entity.course.CourseStatus;
import com.elearning.ProjetPfe.entity.engagement.DetectionRemark;
import com.elearning.ProjetPfe.entity.payment.PaymentStatus;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.course.CourseRepository;
import com.elearning.ProjetPfe.repository.engagement.DetectionRemarkRepository;
import com.elearning.ProjetPfe.repository.payment.EnrollmentRepository;
import com.elearning.ProjetPfe.repository.engagement.ReviewRepository;
import com.elearning.ProjetPfe.service.mongo.MongoAuditService;

/**
 * Service de calcul analytique :
 *   - Agrège détections + avis + inscriptions par cours
 *   - Génère recommandations pédagogiques (Instructor) et stratégiques (Admin)
 *   - Calcule un score d'engagement pondéré (0-100)
 */
@Service
public class AnalyticsService {

    private static final List<String> FATIGUE_TYPES       = Arrays.asList("EYES_CLOSED", "YAWNING", "TIRED");
    private static final List<String> DISTRACTION_TYPES   = Arrays.asList("LOOKING_AWAY", "ABSENT");
    private static final List<String> CONCENTRATION_TYPES = Arrays.asList("CONCENTRATED", "SMILING");

    @Autowired private CourseRepository courseRepo;
    @Autowired private DetectionRemarkRepository detectionRepo;
    @Autowired private ReviewRepository reviewRepo;
    @Autowired private EnrollmentRepository enrollmentRepo;
    @Autowired private MongoAuditService mongoAuditService;

    // ═══════════════════════════════════════════════════════════════════════
    // INSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Retourne les analytics de tous les cours publiés de l'instructor.
     */
    public List<CourseAnalyticsDto> getInstructorAnalytics(User instructor) {
        List<Course> courses = courseRepo.findByInstructorId(instructor.getId()).stream()
                .filter(c -> c.getStatus() == CourseStatus.PUBLISHED)
                .collect(Collectors.toList());

        return courses.stream()
                .map(this::buildCourseAnalytics)
                .sorted(Comparator.comparingDouble(CourseAnalyticsDto::getEngagementScore).reversed())
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ADMIN
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Retourne les analytics globaux de la plateforme (tous cours publiés).
     */
    public AdminAnalyticsDto getAdminAnalytics() {
        List<Course> allPublished = courseRepo.findByStatus(CourseStatus.PUBLISHED);
        List<CourseAnalyticsDto> courses = allPublished.stream()
                .map(this::buildCourseAnalytics)
                .sorted(Comparator.comparingDouble(CourseAnalyticsDto::getEngagementScore).reversed())
                .collect(Collectors.toList());

        AdminAnalyticsDto dto = new AdminAnalyticsDto();
        dto.setAllCourses(courses);

        // Platform aggregates
        computePlatformStats(courses, dto);

        // Ranked sub-lists
        dto.setTopEngagementCourses(courses.stream()
                .sorted(Comparator.comparingDouble(CourseAnalyticsDto::getEngagementScore).reversed())
                .limit(6).collect(Collectors.toList()));

        dto.setTopRatedCourses(courses.stream()
                .filter(c -> c.getAvgRating() != null && c.getReviewCount() >= 1)
                .sorted(Comparator.comparingDouble((CourseAnalyticsDto c) ->
                        c.getAvgRating() == null ? 0.0 : c.getAvgRating()).reversed())
                .limit(6).collect(Collectors.toList()));

        dto.setRecommendedForFeatured(courses.stream()
                .filter(c -> c.getEngagementScore() >= 65
                        && (c.getAvgRating() == null || c.getAvgRating() >= 3.5))
                .limit(6).collect(Collectors.toList()));

        dto.setNeedsImprovementCourses(courses.stream()
                .filter(c -> c.getEngagementScore() < 40 && c.getTotalDetections() > 0)
                .sorted(Comparator.comparingDouble(CourseAnalyticsDto::getEngagementScore))
                .limit(6).collect(Collectors.toList()));

        return dto;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // CORE: bâtir les analytics d'un cours
    // ═══════════════════════════════════════════════════════════════════════

    private CourseAnalyticsDto buildCourseAnalytics(Course course) {
        CourseAnalyticsDto dto = new CourseAnalyticsDto();
        dto.setCourseId(course.getId());
        dto.setTitle(course.getTitle());
        dto.setCoverImage(course.getCoverImage());
        dto.setInstructorName(course.getInstructor() != null ? course.getInstructor().getFullName() : "");

        // Enrollments
        long enrollments = enrollmentRepo.countByCourseIdAndPaymentStatus(course.getId(), PaymentStatus.PAID);
        dto.setEnrollmentCount(enrollments);

        // Reviews
        Double avgRating = reviewRepo.calculateAverageRating(course.getId());
        long reviewCount = reviewRepo.countByCourseId(course.getId());
        dto.setAvgRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : null);
        dto.setReviewCount(reviewCount);

        // Detection remarks
        List<DetectionRemark> remarks = detectionRepo.findByCourseIdOrderByDetectedAtDesc(course.getId());
        computeDetectionStats(remarks, dto);

        // Engagement score (composite)
        dto.setEngagementScore(computeEngagementScore(dto));

        // Recommendation
        buildRecommendation(dto);

        mongoAuditService.logRecommendationEvent(
            dto.getCourseId(),
            dto.getRecommendationType(),
            dto.getRecommendationBadge(),
            dto.getEngagementScore(),
            dto.getTotalDetections(),
            dto.getEnrollmentCount(),
            dto.getAvgRating()
        );

        return dto;
    }

    private void computeDetectionStats(List<DetectionRemark> remarks, CourseAnalyticsDto dto) {
        int total = remarks.size();
        dto.setTotalDetections(total);

        if (total == 0) {
            dto.setAvgAttentionScore(0);
            dto.setConcentrationRate(0);
            dto.setFatigueRate(0);
            dto.setDistractionRate(0);
            dto.setDetectionBreakdown(new HashMap<>());
            return;
        }

        // Breakdown by type
        Map<String, Integer> breakdown = new HashMap<>();
        double sumAttention = 0;
        int attentionCount = 0;

        for (DetectionRemark r : remarks) {
            breakdown.merge(r.getRemarkType(), 1, Integer::sum);
            if (r.getAttentionScore() != null) {
                sumAttention += r.getAttentionScore();
                attentionCount++;
            }
        }
        dto.setDetectionBreakdown(breakdown);

        // Avg attention
        dto.setAvgAttentionScore(attentionCount > 0
                ? Math.round(sumAttention / attentionCount * 10.0) / 10.0 : 0);

        // Rates
        int fatigueCount = FATIGUE_TYPES.stream()
                .mapToInt(t -> breakdown.getOrDefault(t, 0)).sum();
        int distractionCount = DISTRACTION_TYPES.stream()
                .mapToInt(t -> breakdown.getOrDefault(t, 0)).sum();
        int concentrationCount = CONCENTRATION_TYPES.stream()
                .mapToInt(t -> breakdown.getOrDefault(t, 0)).sum();

        dto.setFatigueRate(Math.round(fatigueCount * 100.0 / total * 10.0) / 10.0);
        dto.setDistractionRate(Math.round(distractionCount * 100.0 / total * 10.0) / 10.0);
        dto.setConcentrationRate(Math.round(concentrationCount * 100.0 / total * 10.0) / 10.0);
    }

    private double computeEngagementScore(CourseAnalyticsDto dto) {
        // Weight: 40% attention, 35% rating, 25% enrollment
        double attentionContrib;
        if (dto.getTotalDetections() == 0) {
            attentionContrib = 50.0; // neutral default when no data
        } else {
            attentionContrib = dto.getAvgAttentionScore() * 0.40;
        }

        double ratingContrib;
        if (dto.getAvgRating() == null) {
            ratingContrib = 50.0 * 0.35; // neutral default
        } else {
            ratingContrib = (dto.getAvgRating() / 5.0 * 100.0) * 0.35;
        }

        double normalizedEnrollment = Math.min(100, dto.getEnrollmentCount() / 50.0 * 100.0);
        double enrollmentContrib = normalizedEnrollment * 0.25;

        return Math.round((attentionContrib + ratingContrib + enrollmentContrib) * 10.0) / 10.0;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // RECOMMENDATION ENGINE
    // ═══════════════════════════════════════════════════════════════════════

    private void buildRecommendation(CourseAnalyticsDto dto) {
        boolean hasDetections = dto.getTotalDetections() > 0;
        boolean hasReviews   = dto.getReviewCount() >= 3;
        double avg       = dto.getAvgAttentionScore();
        double fatigue   = dto.getFatigueRate();
        double distract  = dto.getDistractionRate();
        Double rating    = dto.getAvgRating();

        if (!hasDetections && !hasReviews) {
            dto.setRecommendationType("NO_DATA");
            dto.setRecommendationBadge("Pas de données");
            dto.setRecommendationColor("#6b7280");
            dto.setRecommendationIcon("📊");
            dto.setRecommendationMessage(
                "Pas encore de données de suivi disponibles pour ce cours. " +
                "Encouragez vos étudiants à activer le suivi caméra pour obtenir des insights pédagogiques précis.");
            return;
        }

        // Case 1 — Excellent
        if (avg >= 70 && (rating == null || rating >= 4.0) && fatigue < 25 && distract < 20) {
            dto.setRecommendationType("EXCELLENT");
            dto.setRecommendationBadge("Excellent");
            dto.setRecommendationColor("#22c55e");
            dto.setRecommendationIcon("🏆");
            dto.setRecommendationMessage(
                "Les étudiants sont fortement engagés et concentrés pendant ce cours. " +
                "La pédagogie utilisée semble très efficace. Continuez avec cette approche d'enseignement — " +
                "ce cours représente un excellent exemple de qualité sur la plateforme.");
            return;
        }

        // Case 2 — Fatigue élevée
        if (hasDetections && fatigue > 30) {
            dto.setRecommendationType("SEGMENT_CONTENT");
            dto.setRecommendationBadge("Fatigue détectée");
            dto.setRecommendationColor("#f59e0b");
            dto.setRecommendationIcon("😴");
            dto.setRecommendationMessage(
                "Beaucoup d'étudiants montrent des signes de fatigue pendant ce cours (" + (int)fatigue + "% des détections). " +
                "Nous recommandons de :<br>" +
                "• Diviser le contenu en segments de 10-15 minutes maximum<br>" +
                "• Ajouter des pauses pédagogiques entre les sections<br>" +
                "• Introduire des exercices interactifs pour maintenir l'énergie<br>" +
                "• Varier le rythme et le format des explications");
            return;
        }

        // Case 3 — Avis négatifs
        if (hasReviews && rating != null && rating < 3.3) {
            dto.setRecommendationType("SLOW_DOWN");
            dto.setRecommendationBadge("Avis négatifs");
            dto.setRecommendationColor("#ef4444");
            dto.setRecommendationIcon("📝");
            dto.setRecommendationMessage(
                "Les étudiants rencontrent des difficultés sur ce cours (note " + String.format("%.1f", rating) + "/5). " +
                "Nous recommandons de :<br>" +
                "• Ralentir le rythme des explications dans les sections complexes<br>" +
                "• Ajouter des exemples pratiques et des cas concrets<br>" +
                "• Proposer des quiz de vérification à la fin de chaque section<br>" +
                "• Envisager une session de questions-réponses en direct");
            return;
        }

        // Case 4 — Distraction / faible engagement
        if (hasDetections && (distract > 35 || avg < 50)) {
            dto.setRecommendationType("NEEDS_ENGAGEMENT");
            dto.setRecommendationBadge("Engagement faible");
            dto.setRecommendationColor("#3b82f6");
            dto.setRecommendationIcon("🎯");
            dto.setRecommendationMessage(
                "L'attention des étudiants a tendance à se disperser pendant ce cours " +
                "(" + (int)distract + "% de distractions, attention moyenne " + (int)avg + "%). " +
                "Suggestions :<br>" +
                "• Ajouter des questions de vérification interactives au fil du cours<br>" +
                "• Utiliser des exemples visuels et des démonstrations pratiques<br>" +
                "• Commencer chaque section par un objectif clair<br>" +
                "• Varier les formats (vidéo, quiz, exercice, lecture)");
            return;
        }

        // Default — En progression
        dto.setRecommendationType("ON_TRACK");
        dto.setRecommendationBadge("En progression");
        dto.setRecommendationColor("#6366f1");
        dto.setRecommendationIcon("📈");
        dto.setRecommendationMessage(
            "Ce cours progresse bien. Quelques ajustements peuvent encore améliorer l'expérience :<br>" +
            "• Continuez à collecter des données de suivi<br>" +
            "• Encouragez les étudiants à laisser des avis détaillés<br>" +
            "• Maintenez un rythme régulier de mises à jour du contenu");
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PLATFORM STATS
    // ═══════════════════════════════════════════════════════════════════════

    private void computePlatformStats(List<CourseAnalyticsDto> courses, AdminAnalyticsDto dto) {
        List<CourseAnalyticsDto> withDetections = courses.stream()
                .filter(c -> c.getTotalDetections() > 0)
                .collect(Collectors.toList());

        dto.setPlatformTotalDetections(
                courses.stream().mapToInt(CourseAnalyticsDto::getTotalDetections).sum());

        dto.setPlatformTotalEnrollments(
                courses.stream().mapToLong(CourseAnalyticsDto::getEnrollmentCount).sum());

        if (!withDetections.isEmpty()) {
            dto.setPlatformAvgAttention(Math.round(
                    withDetections.stream().mapToDouble(CourseAnalyticsDto::getAvgAttentionScore)
                            .average().orElse(0) * 10.0) / 10.0);
            dto.setPlatformConcentrationRate(Math.round(
                    withDetections.stream().mapToDouble(CourseAnalyticsDto::getConcentrationRate)
                            .average().orElse(0) * 10.0) / 10.0);
            dto.setPlatformFatigueRate(Math.round(
                    withDetections.stream().mapToDouble(CourseAnalyticsDto::getFatigueRate)
                            .average().orElse(0) * 10.0) / 10.0);
            dto.setPlatformDistractionRate(Math.round(
                    withDetections.stream().mapToDouble(CourseAnalyticsDto::getDistractionRate)
                            .average().orElse(0) * 10.0) / 10.0);
        }

        List<CourseAnalyticsDto> withRating = courses.stream()
                .filter(c -> c.getAvgRating() != null)
                .collect(Collectors.toList());

        if (!withRating.isEmpty()) {
            dto.setPlatformAvgRating(Math.round(
                    withRating.stream().mapToDouble(c -> c.getAvgRating()).average().orElse(0)
                    * 10.0) / 10.0);
        }
    }
}
