package com.elearning.ProjetPfe.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.elearning.ProjetPfe.dto.OrderHistoryDto;
import com.elearning.ProjetPfe.entity.Enrollment;
import com.elearning.ProjetPfe.entity.User;
import com.elearning.ProjetPfe.repository.EnrollmentRepository;

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
        // Prix effectif au moment de l'achat (promotions incluses)
        dto.setAmountPaid(e.getCourse().getEffectivePrice());
        dto.setPaymentStatus(e.getPaymentStatus().name());
        dto.setPaidAt(e.getPaidAt());
        dto.setCreatedAt(e.getCreatedAt());
        return dto;
    }
}

