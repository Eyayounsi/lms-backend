package com.elearning.ProjetPfe.service.payment;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.elearning.ProjetPfe.dto.payment.OrderHistoryDto;
import com.elearning.ProjetPfe.entity.payment.Enrollment;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.payment.EnrollmentRepository;

/**
 * Service pour l'historique des commandes d'un étudiant.
 *
 * Règles :
 *  - Retourne TOUTES les commandes (PENDING, PAID, FAILED)
 *  - Un étudiant ne voit QUE ses propres commandes
 *  - Le montant affiché est le prix effectif payé (promotions incluses)
 */
@Service
public class OrderService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    // ═══════════════════════════════════════════════════════════════════════
    //  HISTORIQUE des commandes de l'étudiant connecté
    // ═══════════════════════════════════════════════════════════════════════

    public List<OrderHistoryDto> getMyOrders(User student) {
        // Récupère tous les enrollments de l'étudiant (tous statuts confondus)
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());

        return enrollments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ─── Conversion ────────────────────────────────────────────────────────
    private OrderHistoryDto toDto(Enrollment e) {
        OrderHistoryDto dto = new OrderHistoryDto();
        dto.setEnrollmentId(e.getId());
        dto.setCourseId(e.getCourse().getId());
        dto.setCourseTitle(e.getCourse().getTitle());
        dto.setCourseCoverImage(e.getCourse().getCoverImage());
        dto.setInstructorName(e.getCourse().getInstructor().getFullName());
        dto.setAmount(e.getCourse().getEffectivePrice());
        dto.setStatus(e.getPaymentStatus().name());
        dto.setPurchaseDate(e.getCreatedAt() != null ? e.getCreatedAt().format(FMT) : null);
        return dto;
    }
}

