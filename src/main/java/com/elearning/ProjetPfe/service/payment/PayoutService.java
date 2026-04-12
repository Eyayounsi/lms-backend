package com.elearning.ProjetPfe.service.payment;

import com.elearning.ProjetPfe.service.communication.EmailService;
import com.elearning.ProjetPfe.service.communication.NotificationService;
import com.elearning.ProjetPfe.entity.communication.Notification;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elearning.ProjetPfe.dto.payment.PayoutDto;
import com.elearning.ProjetPfe.entity.communication.NotificationType;
import com.elearning.ProjetPfe.entity.payment.Payout;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.payment.InstructorRevenueRepository;
import com.elearning.ProjetPfe.repository.payment.PayoutRepository;

/**
 * Service de gestion des virements (payouts).
 *
 * Règles métier :
 *   - Un instructor peut demander un virement de tout son solde disponible.
 *   - Solde disponible = totalEarnings − montants déjà PENDING ou PAID.
 *   - L'admin peut marquer un virement comme PAID (avec notes optionnelles)
 *     ou le rejeter (REJECTED) avec une raison.
 *   - Un virement ne peut être traité que s'il est en statut PENDING.
 */
@Service
public class PayoutService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private PayoutRepository payoutRepository;

    @Autowired
    private InstructorRevenueRepository revenueRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    // ═══════════════════════════════════════════════════════════════════════
    //  INSTRUCTOR — calculer le solde disponible
    // ═══════════════════════════════════════════════════════════════════════

    public BigDecimal getAvailableBalance(User instructor) {
        BigDecimal totalEarnings = revenueRepository.sumInstructorAmountByInstructorId(instructor.getId());
        BigDecimal reserved = payoutRepository.sumReservedByInstructorId(instructor.getId());
        BigDecimal balance = totalEarnings.subtract(reserved);
        return balance.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : balance;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  INSTRUCTOR — demander un virement
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public PayoutDto requestPayout(User instructor, String period) {
        BigDecimal available = getAvailableBalance(instructor);
        if (available.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Solde disponible insuffisant pour demander un virement.");
        }

        Payout payout = new Payout();
        payout.setInstructor(instructor);
        payout.setAmount(available);
        payout.setPeriod(period != null && !period.isBlank() ? period : "ALL");
        payout.setStatus("PENDING");

        return toDto(payoutRepository.save(payout));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  INSTRUCTOR — historique de ses virements
    // ═══════════════════════════════════════════════════════════════════════

    public List<PayoutDto> getMyPayouts(User instructor) {
        return payoutRepository.findByInstructorIdOrderByRequestedAtDesc(instructor.getId())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Récupérer un virement par id
    // ═══════════════════════════════════════════════════════════════════════

    public PayoutDto getById(Long id) {
        Payout payout = payoutRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Virement introuvable."));
        return toDto(payout);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ADMIN — liste tous les virements (optionnel : filtre par statut)
    // ═══════════════════════════════════════════════════════════════════════

    public List<PayoutDto> getAllPayouts(String status) {
        List<Payout> list = (status != null && !status.isBlank())
                ? payoutRepository.findByStatusOrderByRequestedAtDesc(status)
                : payoutRepository.findAllByOrderByRequestedAtDesc();
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ADMIN — approuver un virement (PENDING → PAID)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public PayoutDto markAsPaid(Long id, String notes) {
        Payout payout = payoutRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Virement introuvable."));
        if (!"PENDING".equals(payout.getStatus())) {
            throw new IllegalStateException("Seul un virement PENDING peut être marqué PAID.");
        }
        payout.setStatus("PAID");
        payout.setPaidAt(java.time.LocalDateTime.now());
        if (notes != null && !notes.isBlank()) payout.setNotes(notes);
        PayoutDto dto = toDto(payoutRepository.save(payout));

        // Notification in-app
        notificationService.send(
            payout.getInstructor(),
            NotificationType.PAYOUT_PAID,
            "💳 Virement effectué !",
            "Votre virement de " + payout.getAmount() + " € (période : " + payout.getPeriod() + ") a été effectué.",
            "/instructor/instructor-payouts"
        );

        // Email
        emailService.sendPayoutPaid(
            payout.getInstructor().getEmail(),
            payout.getInstructor().getFullName(),
            payout.getAmount(),
            payout.getPeriod(),
            dto.getPaidAt(),
            notes
        );

        return dto;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ADMIN — rejeter un virement (PENDING → REJECTED)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public PayoutDto rejectPayout(Long id, String notes) {
        Payout payout = payoutRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Virement introuvable."));
        if (!"PENDING".equals(payout.getStatus())) {
            throw new IllegalStateException("Seul un virement PENDING peut être rejeté.");
        }
        payout.setStatus("REJECTED");
        if (notes != null && !notes.isBlank()) payout.setNotes(notes);
        PayoutDto dto = toDto(payoutRepository.save(payout));

        // Notification in-app
        notificationService.send(
            payout.getInstructor(),
            NotificationType.PAYOUT_REJECTED,
            "❌ Virement rejeté",
            "Votre demande de virement de " + payout.getAmount() + " € a été rejetée. Raison : " + (notes != null ? notes : "Non précisée"),
            "/instructor/instructor-payouts"
        );

        // Email
        emailService.sendPayoutRejected(
            payout.getInstructor().getEmail(),
            payout.getInstructor().getFullName(),
            payout.getAmount(),
            payout.getPeriod(),
            notes
        );

        return dto;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Conversion entité → DTO
    // ═══════════════════════════════════════════════════════════════════════

    private PayoutDto toDto(Payout p) {
        PayoutDto dto = new PayoutDto();
        dto.setId(p.getId());
        dto.setInstructorId(p.getInstructor().getId());
        dto.setInstructorName(p.getInstructor().getFullName());
        dto.setInstructorEmail(p.getInstructor().getEmail());
        dto.setAmount(p.getAmount());
        dto.setPeriod(p.getPeriod());
        dto.setStatus(p.getStatus());
        dto.setNotes(p.getNotes());
        dto.setRequestedAt(p.getRequestedAt() != null ? p.getRequestedAt().format(FMT) : null);
        dto.setPaidAt(p.getPaidAt() != null ? p.getPaidAt().format(FMT) : null);
        return dto;
    }
}
