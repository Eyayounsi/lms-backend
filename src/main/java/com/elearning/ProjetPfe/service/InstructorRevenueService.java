package com.elearning.ProjetPfe.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elearning.ProjetPfe.dto.InstructorRevenueDto;
import com.elearning.ProjetPfe.entity.Enrollment;
import com.elearning.ProjetPfe.entity.InstructorRevenue;
import com.elearning.ProjetPfe.entity.NotificationType;
import com.elearning.ProjetPfe.entity.User;
import com.elearning.ProjetPfe.repository.InstructorRevenueRepository;

/**
 * Service de gestion des revenus instructor.
 *
 * Règles :
 *  - Commission plateforme configurable (défaut 30%)
 *  - Calculée à chaque paiement confirmé
 *  - Ajustée (ligne REFUND) en cas de remboursement
 *  - Un instructor ne voit QUE ses revenus
 */
@Service
public class InstructorRevenueService {

    /** Taux de commission de la plateforme — configurable dans application.properties */
    @Value("${app.platform.commission.rate:0.30}")
    private BigDecimal platformCommissionRate;

    @Autowired
    private InstructorRevenueRepository revenueRepository;

    @Autowired
    private NotificationService notificationService;

    // ═══════════════════════════════════════════════════════════════════════
    //  ENREGISTRER une vente (appelé par PaymentService après webhook PAID)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public void recordSale(Enrollment enrollment) {
        // Éviter les doublons si le webhook est appelé deux fois
        if (revenueRepository.findByEnrollmentIdAndTransactionType(enrollment.getId(), "SALE").isPresent()) {
            return;
        }

        BigDecimal totalAmount = enrollment.getCourse().getEffectivePrice();
        BigDecimal commission = totalAmount.multiply(platformCommissionRate)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal instructorAmount = totalAmount.subtract(commission);

        InstructorRevenue revenue = new InstructorRevenue();
        revenue.setInstructor(enrollment.getCourse().getInstructor());
        revenue.setCourse(enrollment.getCourse());
        revenue.setEnrollment(enrollment);
        revenue.setTotalAmount(totalAmount);
        revenue.setPlatformCommissionRate(platformCommissionRate);
        revenue.setPlatformCommission(commission);
        revenue.setInstructorAmount(instructorAmount);
        revenue.setTransactionType("SALE");
        revenue.setRevenueMonth(currentMonth());

        revenueRepository.save(revenue);

        // Notifier l'instructor
        User instructor = enrollment.getCourse().getInstructor();
        notificationService.send(
                instructor,
                NotificationType.NEW_REVENUE,
                "💰 Nouvelle vente !",
                "L'étudiant " + enrollment.getStudent().getFullName() +
                        " a acheté votre cours \"" + enrollment.getCourse().getTitle() +
                        "\". Revenu net : " + instructorAmount + " €",
                "/instructor/revenue"
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ENREGISTRER un remboursement (montants négatifs)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public void recordRefund(Enrollment enrollment) {
        // Éviter les doublons
        if (revenueRepository.findByEnrollmentIdAndTransactionType(enrollment.getId(), "REFUND").isPresent()) {
            return;
        }

        BigDecimal totalAmount = enrollment.getCourse().getEffectivePrice().negate();
        BigDecimal commission = totalAmount.multiply(platformCommissionRate)
                .setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal instructorAmount = totalAmount.subtract(commission);

        InstructorRevenue refund = new InstructorRevenue();
        refund.setInstructor(enrollment.getCourse().getInstructor());
        refund.setCourse(enrollment.getCourse());
        refund.setEnrollment(enrollment);
        refund.setTotalAmount(totalAmount);
        refund.setPlatformCommissionRate(platformCommissionRate);
        refund.setPlatformCommission(commission);
        refund.setInstructorAmount(instructorAmount);
        refund.setTransactionType("REFUND");
        refund.setRevenueMonth(currentMonth());

        revenueRepository.save(refund);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TABLEAU DE BORD revenus de l'instructor connecté
    // ═══════════════════════════════════════════════════════════════════════

    public InstructorRevenueDto getMyRevenueDashboard(User instructor) {
        Long instructorId = instructor.getId();
        String currentMonth = currentMonth();

        InstructorRevenueDto dto = new InstructorRevenueDto();

        // Revenu total net
        dto.setTotalRevenue(revenueRepository.sumInstructorAmountByInstructorId(instructorId));

        // Revenu du mois courant
        dto.setCurrentMonthRevenue(
                revenueRepository.sumInstructorAmountByInstructorIdAndMonth(instructorId, currentMonth));

        // Nombre total d'étudiants uniques
        dto.setTotalStudents(revenueRepository.countStudentsByInstructorId(instructorId));

        // Revenus par cours
        List<InstructorRevenueDto.CourseRevenueItem> perCourse = revenueRepository
                .revenuePerCourse(instructorId)
                .stream()
                .map(row -> new InstructorRevenueDto.CourseRevenueItem(
                        (Long) row[0],
                        (String) row[1],
                        (BigDecimal) row[2]
                ))
                .collect(Collectors.toList());
        dto.setPerCourse(perCourse);

        // Historique mensuel
        List<InstructorRevenueDto.MonthlyRevenueItem> monthly = revenueRepository
                .monthlyRevenueByInstructorId(instructorId)
                .stream()
                .map(row -> new InstructorRevenueDto.MonthlyRevenueItem(
                        (String) row[0],
                        (BigDecimal) row[1]
                ))
                .collect(Collectors.toList());
        dto.setMonthly(monthly);

        return dto;
    }

    // ─── Utilitaire : mois courant au format "YYYY-MM" ────────────────────
    private String currentMonth() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }
}

