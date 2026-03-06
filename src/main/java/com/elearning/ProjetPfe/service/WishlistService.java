package com.elearning.ProjetPfe.service;

import com.elearning.ProjetPfe.dto.CartItemDto;
import com.elearning.ProjetPfe.entity.Course;
import com.elearning.ProjetPfe.entity.CourseStatus;
import com.elearning.ProjetPfe.entity.User;
import com.elearning.ProjetPfe.entity.WishlistItem;
import com.elearning.ProjetPfe.repository.CartItemRepository;
import com.elearning.ProjetPfe.repository.CourseRepository;
import com.elearning.ProjetPfe.repository.WishlistItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Logique métier pour la liste de souhaits.
 */
@Service
public class WishlistService {

    @Autowired
    private WishlistItemRepository wishlistItemRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    // ═══════════════════════════════════════════════════════════════════════
    //  Obtenir la wishlist complète
    // ═══════════════════════════════════════════════════════════════════════

    public List<CartItemDto> getWishlist(User student) {
        return wishlistItemRepository.findByStudentId(student.getId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Vérifier si un cours est dans la wishlist (pour l'icône ♥)
    // ═══════════════════════════════════════════════════════════════════════

    public boolean isInWishlist(Long courseId, User student) {
        return wishlistItemRepository.existsByStudentIdAndCourseId(student.getId(), courseId);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Ajouter à la wishlist
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public void addToWishlist(Long courseId, User student) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Cours non trouvé"));

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new RuntimeException("Ce cours n'est pas disponible");
        }

        if (wishlistItemRepository.existsByStudentIdAndCourseId(student.getId(), courseId)) {
            throw new RuntimeException("Ce cours est déjà dans votre liste de souhaits");
        }

        WishlistItem item = new WishlistItem();
        item.setStudent(student);
        item.setCourse(course);
        wishlistItemRepository.save(item);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Retirer de la wishlist
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public void removeFromWishlist(Long courseId, User student) {
        wishlistItemRepository.deleteByStudentIdAndCourseId(student.getId(), courseId);
    }

    /**
     * Déplacer un cours de la wishlist vers le panier.
     * Utilisé par le bouton "Acheter maintenant" sur la page wishlist.
     *
     * Étapes :
     *   1. Retirer de la wishlist
     *   2. Ajouter au panier (via CartItemRepository directement)
     */
    @Transactional
    public void moveToCart(Long courseId, User student) {
        // 1. Retirer de la wishlist
        wishlistItemRepository.deleteByStudentIdAndCourseId(student.getId(), courseId);

        // 2. Ajouter au panier si pas déjà présent
        if (!cartItemRepository.existsByStudentIdAndCourseId(student.getId(), courseId)) {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Cours non trouvé"));
            com.elearning.ProjetPfe.entity.CartItem cartItem = new com.elearning.ProjetPfe.entity.CartItem();
            cartItem.setStudent(student);
            cartItem.setCourse(course);
            cartItemRepository.save(cartItem);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CONVERSION — on réutilise CartItemDto pour la wishlist (même structure)
    // ═══════════════════════════════════════════════════════════════════════

    private CartItemDto toDto(WishlistItem item) {
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
