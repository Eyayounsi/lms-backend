package com.elearning.ProjetPfe.controller.admin;

import com.elearning.ProjetPfe.entity.admin.Ticket;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.dto.admin.TicketDto;
import com.elearning.ProjetPfe.service.admin.TicketService;

/**
 * Admin — gestion des tickets de support.
 *
 * GET    /api/admin/tickets              → tous les tickets
 * GET    /api/admin/tickets?status=XYZ   → filtrés par statut
 * GET    /api/admin/tickets/{id}         → détail
 * PUT    /api/admin/tickets/{id}/status  → changer le statut
 * PUT    /api/admin/tickets/{id}/reply   → répondre
 * DELETE /api/admin/tickets/{id}         → supprimer
 */
@RestController
@RequestMapping("/api/admin/tickets")
public class AdminTicketController {

    @Autowired
    private TicketService ticketService;

    @GetMapping
    public ResponseEntity<List<TicketDto>> getAll(
            @RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return ResponseEntity.ok(ticketService.getTicketsByStatus(status));
        }
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<TicketDto> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.isBlank())
            return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(ticketService.updateStatus(id, status));
    }

    @PutMapping("/{id}/reply")
    public ResponseEntity<TicketDto> reply(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reply = body.get("reply");
        if (reply == null || reply.isBlank())
            return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(ticketService.addAdminReply(id, reply));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        ticketService.adminDeleteTicket(id);
        return ResponseEntity.ok("Ticket supprimé");
    }
}
