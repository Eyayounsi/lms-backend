package com.elearning.ProjetPfe.repository.payment;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.payment.Payout;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, Long> {

    /** Tous les virements d'un instructor, du plus récent au plus ancien */
    List<Payout> findByInstructorIdOrderByRequestedAtDesc(Long instructorId);

    /** Tous les virements, du plus récent au plus ancien (vue admin) */
    List<Payout> findAllByOrderByRequestedAtDesc();

    /** Virements filtrés par statut (vue admin) */
    List<Payout> findByStatusOrderByRequestedAtDesc(String status);

    /**
     * Montant total déjà réservé ou versé pour un instructor
     * (somme des virements PENDING + PAID — exclut REJECTED).
     * Permet de calculer le solde disponible.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payout p " +
           "WHERE p.instructor.id = :instructorId AND p.status IN ('PENDING', 'PAID')")
    BigDecimal sumReservedByInstructorId(@Param("instructorId") Long instructorId);
}
