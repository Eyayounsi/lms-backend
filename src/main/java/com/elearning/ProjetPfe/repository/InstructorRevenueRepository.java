package com.elearning.ProjetPfe.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.InstructorRevenue;

@Repository
public interface InstructorRevenueRepository extends JpaRepository<InstructorRevenue, Long> {

    /** Toutes les lignes de revenu d'un instructor (triées par date DESC) */
    List<InstructorRevenue> findByInstructorIdOrderByCreatedAtDesc(Long instructorId);

    /** Revenu mensuel d'un instructor pour un mois donné (ex: "2026-02") */
    List<InstructorRevenue> findByInstructorIdAndRevenueMonth(Long instructorId, String revenueMonth);

    /** Revenu total net d'un instructor (somme des instructorAmount) */
    @Query("SELECT COALESCE(SUM(r.instructorAmount), 0) FROM InstructorRevenue r WHERE r.instructor.id = :instructorId")
    java.math.BigDecimal sumInstructorAmountByInstructorId(@Param("instructorId") Long instructorId);

    /** Revenu mensuel total net d'un instructor */
    @Query("SELECT COALESCE(SUM(r.instructorAmount), 0) FROM InstructorRevenue r WHERE r.instructor.id = :instructorId AND r.revenueMonth = :month")
    java.math.BigDecimal sumInstructorAmountByInstructorIdAndMonth(
            @Param("instructorId") Long instructorId,
            @Param("month") String month
    );

    /** Nombre total d'étudiants uniques (enrollments) d'un instructor */
    @Query("SELECT COUNT(DISTINCT r.enrollment.id) FROM InstructorRevenue r WHERE r.instructor.id = :instructorId AND r.transactionType = 'SALE'")
    long countStudentsByInstructorId(@Param("instructorId") Long instructorId);

    /** Stats par cours : titre + revenu total instructor */
    @Query("SELECT r.course.id, r.course.title, COALESCE(SUM(r.instructorAmount), 0) " +
           "FROM InstructorRevenue r WHERE r.instructor.id = :instructorId " +
           "GROUP BY r.course.id, r.course.title ORDER BY SUM(r.instructorAmount) DESC")
    List<Object[]> revenuePerCourse(@Param("instructorId") Long instructorId);

    /** Vérifie si un enrollment a déjà généré une ligne de revenu */
    Optional<InstructorRevenue> findByEnrollmentIdAndTransactionType(Long enrollmentId, String transactionType);

    /** Tous les revenus par mois (agrégat mensuel) pour un instructor */
    @Query("SELECT r.revenueMonth, COALESCE(SUM(r.instructorAmount), 0) " +
           "FROM InstructorRevenue r WHERE r.instructor.id = :instructorId " +
           "GROUP BY r.revenueMonth ORDER BY r.revenueMonth DESC")
    List<Object[]> monthlyRevenueByInstructorId(@Param("instructorId") Long instructorId);
}

