package com.elearning.ProjetPfe.controller.student;

import com.elearning.ProjetPfe.dto.payment.CartItemDto;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.service.payment.WishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints pour la liste de souhaits.
 *
 * ─── ENDPOINTS ────────────────────────────────────────────────────────────
 *
 *   GET  /api/student/wishlist
 *     → Voir toute la wishlist
 *
 *   POST /api/student/wishlist/{courseId}
 *     → Ajouter un cours à la wishlist
 *
 *   DELETE /api/student/wishlist/{courseId}
 *     → Retirer un cours de la wishlist
 *
 *   GET  /api/student/wishlist/{courseId}/check
 *     → Vérifier si un cours est dans la wishlist
 *     → Retourne: { "inWishlist": true }
 *     → Utilisé pour changer l'icône ♥ (vide → plein)
 *
 *   POST /api/student/wishlist/{courseId}/move-to-cart
 *     → Déplacer de la wishlist vers le panier
 */
@RestController
@RequestMapping("/api/student/wishlist")
public class StudentWishlistController {

    @Autowired
    private WishlistService wishlistService;

    @GetMapping
    public ResponseEntity<List<CartItemDto>> getWishlist(
            @AuthenticationPrincipal User student) {
        return ResponseEntity.ok(wishlistService.getWishlist(student));
    }

    @GetMapping("/{courseId}/check")
    public ResponseEntity<Map<String, Boolean>> checkWishlist(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User student) {
        boolean inWishlist = wishlistService.isInWishlist(courseId, student);
        return ResponseEntity.ok(Map.of("inWishlist", inWishlist));
    }

    @PostMapping("/{courseId}")
    public ResponseEntity<String> addToWishlist(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User student) {
        wishlistService.addToWishlist(courseId, student);
        return ResponseEntity.ok("Cours ajouté à la liste de souhaits");
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<String> removeFromWishlist(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User student) {
        wishlistService.removeFromWishlist(courseId, student);
        return ResponseEntity.ok("Cours retiré de la liste de souhaits");
    }

    @PostMapping("/{courseId}/move-to-cart")
    public ResponseEntity<String> moveToCart(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User student) {
        wishlistService.moveToCart(courseId, student);
        return ResponseEntity.ok("Cours déplacé vers le panier");
    }
}
