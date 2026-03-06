package com.elearning.ProjetPfe.controller;

import com.elearning.ProjetPfe.dto.CartItemDto;
import com.elearning.ProjetPfe.entity.User;
import com.elearning.ProjetPfe.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Endpoints pour le panier d'achat.
 *
 * ─── ENDPOINTS ────────────────────────────────────────────────────────────
 *
 *   GET  /api/student/cart
 *     → Voir le panier complet avec les prix effectifs
 *
 *   POST /api/student/cart/{courseId}
 *     → Ajouter un cours au panier
 *
 *   DELETE /api/student/cart/{courseId}
 *     → Retirer un cours du panier
 *
 *   GET  /api/student/cart/count
 *     → Nombre d'articles (pour le badge du header)
 *     → Retourne : { "count": 3 }
 *
 * ═══════════════════════════════════════════════════════════════════════════
 * EXEMPLE POSTMAN :
 * ═══════════════════════════════════════════════════════════════════════════
 *   POST http://localhost:8081/api/student/cart/1
 *   Authorization: Bearer <token étudiant>
 *   → Ajoute le cours #1 au panier
 *
 *   GET http://localhost:8081/api/student/cart
 *   → Retourne la liste des cours dans le panier avec les prix
 */
@RestController
@RequestMapping("/api/student/cart")
public class StudentCartController {

    @Autowired
    private CartService cartService;

    @GetMapping
    public ResponseEntity<List<CartItemDto>> getCart(
            @AuthenticationPrincipal User student) {
        return ResponseEntity.ok(cartService.getCart(student));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getCartCount(
            @AuthenticationPrincipal User student) {
        return ResponseEntity.ok(Map.of("count", cartService.getCartCount(student)));
    }

    @PostMapping("/{courseId}")
    public ResponseEntity<CartItemDto> addToCart(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User student) {
        return ResponseEntity.ok(cartService.addToCart(courseId, student));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<String> removeFromCart(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User student) {
        cartService.removeFromCart(courseId, student);
        return ResponseEntity.ok("Cours retiré du panier");
    }
}
