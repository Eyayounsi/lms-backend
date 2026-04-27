package com.elearning.ProjetPfe.controller.student;

import com.elearning.ProjetPfe.entity.communication.Message;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.dto.course.CourseResponseDto;
import com.elearning.ProjetPfe.entity.course.Course;
import com.elearning.ProjetPfe.entity.course.CourseStatus;
import com.elearning.ProjetPfe.entity.payment.Enrollment;
import com.elearning.ProjetPfe.entity.payment.PaymentStatus;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.course.CourseRepository;
import com.elearning.ProjetPfe.repository.payment.EnrollmentRepository;
import com.elearning.ProjetPfe.repository.auth.UserRepository;
import com.elearning.ProjetPfe.service.course.CourseService;

/**
 * Endpoints pour l'étudiant : ses cours achetés.
 */
@RestController
@RequestMapping("/api/student")
public class StudentCourseController {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * GET /api/student/my-courses
     * Retourne la liste des cours que l'étudiant a payés.
     */
    @GetMapping("/my-courses")
    public ResponseEntity<List<CourseResponseDto>> getMyCourses(Authentication authentication) {
        String email = authentication.getName();
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        List<Enrollment> paidEnrollments = enrollmentRepository
                .findByStudentIdAndPaymentStatus(student.getId(), PaymentStatus.PAID);

        List<CourseResponseDto> courses = paidEnrollments.stream()
                .map(enrollment -> courseService.getCourseById(enrollment.getCourse().getId()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(courses);
    }

    /**
     * POST /api/student/enroll-free/{courseId}
     * Inscrit l'étudiant à un cours gratuit (prix effectif = 0).
     * Crée directement un enrollment PAID sans passer par Stripe.
     */
    @PostMapping("/enroll-free/{courseId}")
    public ResponseEntity<?> enrollFree(
            @PathVariable Long courseId,
            Authentication authentication) {

        String email = authentication.getName();
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new RuntimeException("Ce cours n'est pas disponible");
        }

        // Vérifier que le cours est bien gratuit
        BigDecimal effectivePrice = course.getEffectivePrice();
        if (effectivePrice != null && effectivePrice.compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("Ce cours n'est pas gratuit");
        }

        // Vérifier que l'étudiant n'est pas déjà inscrit
        if (enrollmentRepository.findByStudentIdAndCourseIdAndPaymentStatus(
                student.getId(), courseId, PaymentStatus.PAID).isPresent()) {
            return ResponseEntity.ok(Map.of("message", "Vous êtes déjà inscrit à ce cours"));
        }

        // Créer ou mettre à jour l'enrollment → PAID directement
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(student.getId(), courseId)
                .orElse(new Enrollment());

        enrollment.setStudent(student);
        enrollment.setCourse(course);
        enrollment.setPaymentStatus(PaymentStatus.PAID);
        enrollment.setPaidAt(LocalDateTime.now());
        enrollment.setStripeSessionId("FREE_ENROLLMENT");
        enrollmentRepository.save(enrollment);

        return ResponseEntity.ok(Map.of("message", "Inscription réussie !"));
    }
}
