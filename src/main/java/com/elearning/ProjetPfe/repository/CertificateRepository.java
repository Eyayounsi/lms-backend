package com.elearning.ProjetPfe.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.elearning.ProjetPfe.entity.Certificate;

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
}

