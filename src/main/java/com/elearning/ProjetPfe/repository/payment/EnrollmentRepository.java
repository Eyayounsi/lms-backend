package com.elearning.ProjetPfe.repository.payment;

import com.elearning.ProjetPfe.entity.course.Course;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.payment.Enrollment;
import com.elearning.ProjetPfe.entity.payment.PaymentStatus;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    /** Vérifier si un étudiant est inscrit et a payé un cours */
    Optional<Enrollment> findByStudentIdAndCourseIdAndPaymentStatus(
            Long studentId, Long courseId, PaymentStatus paymentStatus);

    /** Vérifier si un enrollment existe déjà (même non payé) */
    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    /** Tous les cours payés par un étudiant */
    List<Enrollment> findByStudentIdAndPaymentStatus(Long studentId, PaymentStatus paymentStatus);

    /** Trouver un enrollment par session Stripe (pour le webhook) */
    Optional<Enrollment> findByStripeSessionId(String stripeSessionId);

    /** Nombre d'étudiants inscrits et payés pour un cours */
    long countByCourseIdAndPaymentStatus(Long courseId, PaymentStatus paymentStatus);

    /** Supprime tous les enrollments d'un cours (utilisé avant la suppression admin) */
    void deleteByCourseId(Long courseId);

    /** Supprime tous les enrollments d'un étudiant (suppression de compte) */
    void deleteByStudentId(Long studentId);

    /** Tous les enrollments d'un étudiant (tous statuts) — historique commandes */
    List<Enrollment> findByStudentId(Long studentId);

    /** Tous les enrollments pour les cours d'un instructor */
    @org.springframework.data.jpa.repository.Query("SELECT e FROM Enrollment e WHERE e.course.instructor.id = :instructorId AND e.paymentStatus = 'PAID'")
    List<Enrollment> findByInstructorId(@org.springframework.data.repository.query.Param("instructorId") Long instructorId);
}
