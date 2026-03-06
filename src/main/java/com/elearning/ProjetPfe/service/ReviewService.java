package com.elearning.ProjetPfe.service;

import com.elearning.ProjetPfe.dto.ReviewDto;
import com.elearning.ProjetPfe.entity.Course;
import com.elearning.ProjetPfe.entity.PaymentStatus;
import com.elearning.ProjetPfe.entity.Review;
import com.elearning.ProjetPfe.entity.User;
import com.elearning.ProjetPfe.repository.CourseRepository;
import com.elearning.ProjetPfe.repository.EnrollmentRepository;
import com.elearning.ProjetPfe.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Logique métier pour les avis sur les cours.
 *
 * ═══════════════════════════════════════════════════
 * RÈGLES IMPORTANTES :
 * ═══════════════════════════════════════════════════
 *   1. L'étudiant DOIT avoir acheté le cours (Enrollment PAID)
 *   2. Un étudiant ne peut laisser qu'UN SEUL avis par cours
 *      → s'il essaie de recréer, on MODIFIE l'avis existant
 *   3. rating doit être entre 1 et 5
 */
@Service
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    // ═══════════════════════════════════════════════════════════════════════
    //  PUBLIC — Voir les avis d'un cours
    // ═══════════════════════════════════════════════════════════════════════

    public List<ReviewDto> getCourseReviews(Long courseId) {
        return reviewRepository.findByCourseIdOrderByCreatedAtDesc(courseId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STUDENT — Créer/Modifier un avis (upsert)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public ReviewDto upsertReview(Long courseId, ReviewDto dto, User student) {
        // 1. Vérifier que l'étudiant a acheté le cours
        enrollmentRepository.findByStudentIdAndCourseIdAndPaymentStatus(
                student.getId(), courseId, PaymentStatus.PAID
        ).orElseThrow(() -> new RuntimeException(
                "Vous devez avoir acheté ce cours pour laisser un avis"
        ));

        // 2. Valider la note
        if (dto.getRating() < 1 || dto.getRating() > 5) {
            throw new RuntimeException("La note doit être entre 1 et 5");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));

        // 3. INSERT ou UPDATE (upsert) — un seul avis par étudiant / cours
        Review review = reviewRepository
                .findByStudentIdAndCourseId(student.getId(), courseId)
                .orElse(new Review()); // nouveau si pas encore d'avis

        review.setStudent(student);
        review.setCourse(course);
        review.setRating(dto.getRating());
        review.setComment(dto.getComment());

        review = reviewRepository.save(review);
        return toDto(review);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STUDENT — Supprimer son propre avis
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public void deleteReview(Long courseId, User student) {
        Review review = reviewRepository
                .findByStudentIdAndCourseId(student.getId(), courseId)
                .orElseThrow(() -> new RuntimeException("Aucun avis trouvé pour ce cours"));

        reviewRepository.delete(review);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CONVERSION
    // ═══════════════════════════════════════════════════════════════════════

    private ReviewDto toDto(Review review) {
        ReviewDto dto = new ReviewDto();
        dto.setId(review.getId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCourseId(review.getCourse().getId());
        dto.setStudentId(review.getStudent().getId());
        dto.setStudentName(review.getStudent().getFullName());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setUpdatedAt(review.getUpdatedAt());
        return dto;
    }
}
