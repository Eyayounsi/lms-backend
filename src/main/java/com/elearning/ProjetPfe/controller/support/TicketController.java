package com.elearning.ProjetPfe.controller.support;

import com.elearning.ProjetPfe.entity.admin.Ticket;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.dto.admin.TicketDto;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.auth.UserRepository;
import com.elearning.ProjetPfe.service.admin.TicketService;

/**
 * Endpoints tickets de support — accessible par tout utilisateur authentifié.
 *
 * GET    /api/user/tickets         → mes tickets
 * POST   /api/user/tickets         → créer un ticket
 * PUT    /api/user/tickets/{id}    → modifier mon ticket
 * DELETE /api/user/tickets/{id}    → supprimer mon ticket
 */
@RestController
@RequestMapping("/api/user/tickets")
public class TicketController {

    @Autowired private TicketService ticketService;
    @Autowired private UserRepository userRepository;

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    @GetMapping
    public ResponseEntity<List<TicketDto>> getMyTickets(Authentication auth) {
        return ResponseEntity.ok(ticketService.getMyTickets(getUser(auth)));
    }

    @PostMapping
    public ResponseEntity<TicketDto> createTicket(
            @RequestBody Map<String, Object> data, Authentication auth) {
        return ResponseEntity.ok(ticketService.createTicket(data, getUser(auth)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketDto> updateTicket(
            @PathVariable Long id,
            @RequestBody Map<String, Object> data,
            Authentication auth) {
        return ResponseEntity.ok(ticketService.updateTicket(id, data, getUser(auth)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteTicket(
            @PathVariable Long id, Authentication auth) {
        ticketService.deleteTicket(id, getUser(auth));
        return ResponseEntity.ok("Ticket supprimé");
    }
}
