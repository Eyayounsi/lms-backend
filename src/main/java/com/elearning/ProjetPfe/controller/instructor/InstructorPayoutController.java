package com.elearning.ProjetPfe.controller.instructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.dto.payment.PayoutDto;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.service.payment.PayoutService;

/**
 * Endpoints instructor pour les virements.
 *
 * GET  /api/instructor/payouts            → historique des virements
 * GET  /api/instructor/payouts/balance    → solde disponible
 * POST /api/instructor/payouts            → demander un virement
 */
@RestController
@RequestMapping("/api/instructor/payouts")
public class InstructorPayoutController {

    @Autowired
    private PayoutService payoutService;

    /** Solde disponible pour virement */
    @GetMapping("/balance")
    public ResponseEntity<Map<String, BigDecimal>> getBalance(
            @AuthenticationPrincipal User instructor) {
        BigDecimal balance = payoutService.getAvailableBalance(instructor);
        return ResponseEntity.ok(Map.of("availableBalance", balance));
    }

    /** Historique des virements de l'instructor */
    @GetMapping
    public ResponseEntity<List<PayoutDto>> getMyPayouts(
            @AuthenticationPrincipal User instructor) {
        return ResponseEntity.ok(payoutService.getMyPayouts(instructor));
    }

    /** Récupère un virement par son id (pour la facture) — vérifie l'appartenance */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(
            @PathVariable Long id,
            @AuthenticationPrincipal User instructor) {
        try {
            PayoutDto dto = payoutService.getById(id);
            if (!dto.getInstructorId().equals(instructor.getId())) {
                return ResponseEntity.status(403).body(java.util.Map.of("error", "Accès refusé."));
            }
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Demander un virement.
     * Body JSON optionnel : { "period": "2026-03" }
     * Si period absent → "ALL" (tout le solde disponible).
     */
    @PostMapping
    public ResponseEntity<?> requestPayout(
            @AuthenticationPrincipal User instructor,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String period = body != null ? body.get("period") : null;
            PayoutDto dto = payoutService.requestPayout(instructor, period);
            return ResponseEntity.ok(dto);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
