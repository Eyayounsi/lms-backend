package com.elearning.ProjetPfe.service.payment;

import com.elearning.ProjetPfe.dto.payment.CartItemDto;
import com.elearning.ProjetPfe.entity.payment.CartItem;
import com.elearning.ProjetPfe.entity.course.Course;
import com.elearning.ProjetPfe.entity.course.CourseStatus;
import com.elearning.ProjetPfe.entity.payment.PaymentStatus;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.payment.CartItemRepository;
import com.elearning.ProjetPfe.repository.course.CourseRepository;
import com.elearning.ProjetPfe.repository.payment.EnrollmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Logique métier pour le panier d'achat.
 *
 * ═══════════════════════════════════════════════════
 * RÈGLES IMPORTANTES :
 * ═══════════════════════════════════════════════════
 *   1. On ne peut pas ajouter un cours déjà acheté au panier
 *   2. On ne peut pas ajouter un cours déjà dans le panier (doublon)
 *   3. Le cours doit être PUBLISHED pour pouvoir être acheté
 *   4. Après paiement Stripe réussi → CartService.clearCart(studentId) est appelé
 *
 * ═══════════════════════════════════════════════════
 * FLOW COMPLET CHECKOUT STRIPE :
 * ═══════════════════════════════════════════════════
 *   1. POST /api/student/cart/{courseId} → ajouter au panier
 *   2. GET  /api/student/cart            → voir le panier (total calculé)
 *   3. POST /api/payment/checkout        → crée une session Stripe
 *   4. Stripe webhook success            → CartService.clearCart() + créer Enrollments
 */
@Service
public class CartService {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    // ═══════════════════════════════════════════════════════════════════════
    //  Obtenir le panier complet
    // ═══════════════════════════════════════════════════════════════════════

    public List<CartItemDto> getCart(User student) {
        List<CartItem> items = cartItemRepository.findByStudentId(student.getId());

        // Auto-clean: remove items for courses already purchased
        List<CartItem> alreadyOwned = items.stream()
                .filter(ci -> enrollmentRepository
                        .findByStudentIdAndCourseIdAndPaymentStatus(
                                student.getId(), ci.getCourse().getId(), PaymentStatus.PAID)
                        .isPresent())
                .toList();
        if (!alreadyOwned.isEmpty()) {
            cartItemRepository.deleteAll(alreadyOwned);
            items.removeAll(alreadyOwned);
        }

        return items.stream().map(this::toDto).collect(Collectors.toList());
    }

    /** Nombre d'articles (pour le badge dans le header) */
    public long getCartCount(User student) {
        return cartItemRepository.countByStudentId(student.getId());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Ajouter un cours au panier
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public CartItemDto addToCart(Long courseId, User student) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));

        // Vérifier que le cours est publié
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new RuntimeException("Ce cours n'est pas disponible à l'achat");
        }

        // Vérifier que l'étudiant n'a pas déjà ce cours
        boolean alreadyEnrolled = enrollmentRepository
                .findByStudentIdAndCourseIdAndPaymentStatus(
                        student.getId(), courseId, PaymentStatus.PAID)
                .isPresent();
        if (alreadyEnrolled) {
            throw new RuntimeException("Vous avez déjà acheté ce cours");
        }

        // Vérifier que le cours n'est pas déjà dans le panier
        if (cartItemRepository.existsByStudentIdAndCourseId(student.getId(), courseId)) {
            throw new RuntimeException("Ce cours est déjà dans votre panier");
        }

        CartItem item = new CartItem();
        item.setStudent(student);
        item.setCourse(course);

        item = cartItemRepository.save(item);
        return toDto(item);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Retirer un cours du panier
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public void removeFromCart(Long courseId, User student) {
        if (!cartItemRepository.existsByStudentIdAndCourseId(student.getId(), courseId)) {
            throw new RuntimeException("Ce cours n'est pas dans votre panier");
        }
        cartItemRepository.deleteByStudentIdAndCourseId(student.getId(), courseId);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Vider le panier (après paiement réussi)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public void clearCart(Long studentId) {
        cartItemRepository.deleteByStudentId(studentId);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CONVERSION
    // ═══════════════════════════════════════════════════════════════════════

    private CartItemDto toDto(CartItem item) {
        Course course = item.getCourse();
        CartItemDto dto = new CartItemDto();
        dto.setId(item.getId());
        dto.setCourseId(course.getId());
        dto.setCourseTitle(course.getTitle());
        dto.setCourseCoverImage(course.getCoverImage());
        dto.setInstructorName(course.getInstructor().getFullName());
        dto.setOriginalPrice(course.getPrice());
        dto.setEffectivePrice(course.getEffectivePrice());
        dto.setOnSale(course.isOnSale());
        dto.setAddedAt(item.getAddedAt());
        return dto;
    }
}
