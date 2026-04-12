package com.elearning.ProjetPfe.repository.learning;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.learning.Certificate;
import com.elearning.ProjetPfe.entity.auth.User;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    /** Vérifie si un certificat existe pour un (étudiant, cours) */
    Optional<Certificate> findByStudentIdAndCourseId(Long studentId, Long courseId);

    /** Tous les certificats d'un étudiant */
    List<Certificate> findByStudentIdOrderByIssuedAtDesc(Long studentId);

    /** Vérification publique par code unique */
    Optional<Certificate> findByCertificateCode(String certificateCode);

    /** Vérifie si le certificat appartient à cet étudiant */
    boolean existsByIdAndStudentId(Long id, Long studentId);

    /** Tous les certificats pour les cours d'un instructeur */
    List<Certificate> findByCourse_InstructorIdOrderByIssuedAtDesc(Long instructorId);

    /**
     * Retourne la liste distincte des étudiants ayant au moins un certificat
     * ET ayant activé le partage de profil avec les recruteurs.
     */
    @Query("SELECT DISTINCT c.student FROM Certificate c WHERE c.student.shareWithRecruiters = true ORDER BY c.student.fullName")
    List<User> findStudentsWithCertificatesAndSharedProfile();
}

