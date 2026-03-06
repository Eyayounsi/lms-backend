package com.elearning.ProjetPfe.controller;

import com.elearning.ProjetPfe.entity.User;
import com.elearning.ProjetPfe.repository.UserRepository;
import com.elearning.ProjetPfe.service.PaymentService;
import com.stripe.exception.StripeException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Contrôleur pour le paiement Stripe.
 *
 * ═══════════════════════════════════════════════════
 * ENDPOINTS :
 * ═══════════════════════════════════════════════════
 *
 *  1) POST /api/payment/create-session
 *     → Appelé par Angular quand le student clique "Acheter ce cours"
 *     → Retourne l'URL de la page de paiement Stripe
 *     → Requiert authentification (JWT)
 *
 *  2) POST /api/payment/webhook
 *     → Appelé par STRIPE (pas par Angular !)
 *     → Stripe envoie une notification après chaque paiement
 *     → PAS de JWT ici (c'est Stripe qui appelle, pas un user)
 *     → On utilise la SIGNATURE Stripe pour vérifier l'authenticité
 *
 * ═══════════════════════════════════════════════════
 */
@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "http://localhost:4200")
public class PaymentController {

    private final PaymentService paymentService;
    private final UserRepository userRepository;

    public PaymentController(PaymentService paymentService,
                             UserRepository userRepository) {
        this.paymentService = paymentService;
        this.userRepository = userRepository;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ENDPOINT 1 : Créer une session de paiement
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Le student envoie : { "courseId": 5 }
     * On retourne    : { "sessionId": "cs_test_...", "url": "https://checkout.stripe.com/..." }
     * Angular redirige ensuite le navigateur vers cette URL.
     */
    @PostMapping("/create-session")
    public ResponseEntity<?> createCheckoutSession(
            @RequestBody Map<String, Long> body,
            Authentication authentication) {

        try {
            Long courseId = body.get("courseId");
            if (courseId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "courseId est requis"));
            }

            // Récupérer l'utilisateur connecté via le token JWT
            String email = authentication.getName();
            User student = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

            // Créer la session Stripe
            Map<String, String> result = paymentService.createCheckoutSession(courseId, student);

            return ResponseEntity.ok(result);

        } catch (StripeException e) {
            // Erreur venant de Stripe (mauvaise clé, réseau, etc.)
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Erreur Stripe: " + e.getMessage()));
        } catch (RuntimeException e) {
            // Erreur métier (cours non trouvé, déjà acheté, etc.)
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ENDPOINT 2 : Webhook Stripe
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Cet endpoint est appelé par STRIPE, pas par le frontend.
     *
     * IMPORTANT : @RequestBody String payload
     * → On reçoit le corps BRUT (pas de JSON parsing)
     * → C'est obligatoire pour vérifier la signature Stripe
     *
     * Le header "Stripe-Signature" contient la signature de Stripe
     * → On la vérifie avec notre webhook secret pour s'assurer
     *    que c'est bien Stripe qui envoie cette requête
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        try {
            paymentService.handleWebhook(payload, sigHeader);
            return ResponseEntity.ok("OK");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ENDPOINT 3 : Vérifier si un cours a été acheté
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Le frontend peut appeler GET /api/payment/check/{courseId}
     * pour savoir si l'étudiant connecté a déjà payé ce cours.
     */
    @GetMapping("/check/{courseId}")
    public ResponseEntity<Map<String, Boolean>> checkPayment(
            @PathVariable Long courseId,
            Authentication authentication) {

        String email = authentication.getName();
        User student = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        boolean paid = paymentService.hasStudentPaidCourse(student.getId(), courseId);
        return ResponseEntity.ok(Map.of("paid", paid));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ENDPOINT 4 : Confirmer le paiement après retour Stripe (fallback webhook)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Appelé par le frontend après que Stripe redirige vers la success URL.
     * Prend le session_id en paramètre, interroge l'API Stripe,
     * et confirme le paiement sans attendre le webhook.
     */
    @PostMapping("/confirm")
    public ResponseEntity<?> confirmPayment(
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        String sessionId = body.get("sessionId");
        if (sessionId == null || sessionId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "sessionId est requis"));
        }

        try {
            paymentService.confirmSessionPayment(sessionId);
            return ResponseEntity.ok(Map.of("status", "confirmed"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Erreur confirmation: " + e.getMessage()));
        }
    }
}
