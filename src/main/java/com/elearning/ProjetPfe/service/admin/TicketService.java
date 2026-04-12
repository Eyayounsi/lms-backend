package com.elearning.ProjetPfe.service.admin;

import com.elearning.ProjetPfe.entity.course.Category;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elearning.ProjetPfe.dto.admin.TicketDto;
import com.elearning.ProjetPfe.entity.admin.Ticket;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.admin.TicketRepository;

@Service
public class TicketService {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    @Autowired
    private TicketRepository ticketRepository;

    // ─── Lister mes tickets ──────────────────────────────────────────────
    public List<TicketDto> getMyTickets(User user) {
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ─── Créer un ticket ─────────────────────────────────────────────────
    @Transactional
    public TicketDto createTicket(Map<String, Object> data, User user) {
        validateTicketData(data);

        Ticket ticket = new Ticket();
        ticket.setUser(user);
        ticket.setSubject(((String) data.get("subject")).trim());
        ticket.setCategory((String) data.get("category"));
        ticket.setPriority((String) data.get("priority"));
        ticket.setStatus("Opened");
        if (data.get("description") != null) {
            ticket.setDescription(((String) data.get("description")).trim());
        }
        return toDto(ticketRepository.save(ticket));
    }

    // ─── Modifier un ticket ───────────────────────────────────────────────
    @Transactional
    public TicketDto updateTicket(Long id, Map<String, Object> data, User user) {
        Ticket ticket = ticketRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Ticket introuvable ou accès refusé"));

        if (data.containsKey("subject") && data.get("subject") != null)
            ticket.setSubject(((String) data.get("subject")).trim());
        if (data.containsKey("category") && data.get("category") != null)
            ticket.setCategory((String) data.get("category"));
        if (data.containsKey("priority") && data.get("priority") != null)
            ticket.setPriority((String) data.get("priority"));
        if (data.containsKey("description"))
            ticket.setDescription(data.get("description") != null
                    ? ((String) data.get("description")).trim() : null);

        return toDto(ticketRepository.save(ticket));
    }

    // ─── Supprimer un ticket (par l'utilisateur) ─────────────────────────
    @Transactional
    public void deleteTicket(Long id, User user) {
        Ticket ticket = ticketRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new RuntimeException("Ticket introuvable ou accès refusé"));
        ticketRepository.delete(ticket);
    }

    // ─── Supprimer un ticket (par l'admin) ───────────────────────────────
    @Transactional
    public void adminDeleteTicket(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket introuvable"));
        ticketRepository.delete(ticket);
    }

    // ─── Mapper ───────────────────────────────────────────────────────────
    private TicketDto toDto(Ticket t) {
        TicketDto dto = new TicketDto();
        dto.setId(t.getId());
        dto.setTicketId(String.format("TIC%03d", t.getId()));
        dto.setDate(t.getCreatedAt() != null ? t.getCreatedAt().format(FORMATTER) : "");
        dto.setSubject(t.getSubject());
        dto.setCategory(t.getCategory());
        dto.setPriority(t.getPriority());
        dto.setStatus(t.getStatus());
        dto.setDescription(t.getDescription());
        dto.setAdminReply(t.getAdminReply());
        dto.setRespondedAt(t.getRespondedAt() != null ? t.getRespondedAt().format(FORMATTER) : null);
        if (t.getUser() != null) {
            dto.setUserName(t.getUser().getFullName());
            dto.setUserEmail(t.getUser().getEmail());
        }
        return dto;
    }

    // ─── Admin : lister tous les tickets ─────────────────────────────────
    public java.util.List<TicketDto> getAllTickets() {
        return ticketRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toDto).collect(java.util.stream.Collectors.toList());
    }

    public java.util.List<TicketDto> getTicketsByStatus(String status) {
        return ticketRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream().map(this::toDto).collect(java.util.stream.Collectors.toList());
    }

    // ─── Admin : changer le statut d'un ticket ────────────────────────────
    @Transactional
    public TicketDto updateStatus(Long id, String status) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket introuvable"));
        ticket.setStatus(status);
        return toDto(ticketRepository.save(ticket));
    }

    // ─── Admin : répondre à un ticket ────────────────────────────────────
    @Transactional
    public TicketDto addAdminReply(Long id, String reply) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket introuvable"));
        ticket.setAdminReply(reply);
        ticket.setRespondedAt(java.time.LocalDateTime.now());
        if ("Opened".equals(ticket.getStatus())) {
            ticket.setStatus("Inprogress");
        }
        return toDto(ticketRepository.save(ticket));
    }

    private void validateTicketData(Map<String, Object> data) {
        if (data.get("subject") == null || data.get("subject").toString().isBlank())
            throw new IllegalArgumentException("Le sujet est obligatoire");
        if (data.get("category") == null || data.get("category").toString().isBlank())
            throw new IllegalArgumentException("La catégorie est obligatoire");
        if (data.get("priority") == null || data.get("priority").toString().isBlank())
            throw new IllegalArgumentException("La priorité est obligatoire");
    }
}
