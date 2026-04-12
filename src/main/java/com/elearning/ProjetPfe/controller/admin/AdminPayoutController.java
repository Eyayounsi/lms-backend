package com.elearning.ProjetPfe.controller.admin;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.dto.payment.PayoutDto;
import com.elearning.ProjetPfe.service.payment.PayoutService;

/**
 * Endpoints admin pour la gestion des virements.
 *
 * GET  /api/admin/payouts?status=         → tous les virements (filtre optionnel)
 * PUT  /api/admin/payouts/{id}/pay        → marquer comme PAID
 * PUT  /api/admin/payouts/{id}/reject     → rejeter avec raison
 */
@RestController
@RequestMapping("/api/admin/payouts")
public class AdminPayoutController {

    @Autowired
    private PayoutService payoutService;

    /** Liste tous les virements, avec filtre optionnel par statut */
    @GetMapping
    public ResponseEntity<List<PayoutDto>> getAll(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(payoutService.getAllPayouts(status));
    }

    /** Récupère un virement par son id (pour la facture) */
    @GetMapping("/{id}")
    public ResponseEntity<?> getOne(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(payoutService.getById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Marquer un virement comme PAID (effectué).
     * Body JSON optionnel : { "notes": "Virement effectué via virement bancaire" }
     */
    @PutMapping("/{id}/pay")
    public ResponseEntity<?> markAsPaid(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String notes = body != null ? body.get("notes") : null;
            return ResponseEntity.ok(payoutService.markAsPaid(id, notes));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Rejeter un virement.
     * Body JSON obligatoire : { "notes": "Raison du rejet" }
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> reject(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String notes = body != null ? body.get("notes") : null;
            return ResponseEntity.ok(payoutService.rejectPayout(id, notes));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
