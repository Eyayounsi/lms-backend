package com.elearning.ProjetPfe.controller.student;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.elearning.ProjetPfe.dto.learning.CertificateDto;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.service.learning.CertificateService;

/**
 * Endpoints certificats pour l'étudiant.
 *
 * GET  /api/student/certificates              → liste mes certificats
 * POST /api/student/certificates/generate/{courseId} → générer (si 100%)
 * GET  /api/student/certificates/{id}         → détail d'un certificat
 */
@RestController
@RequestMapping("/api/student/certificates")
public class StudentCertificateController {

    @Autowired
    private CertificateService certificateService;

    /** Liste tous les certificats de l'étudiant connecté */
    @GetMapping
    public ResponseEntity<List<CertificateDto>> getMyCertificates(
            @AuthenticationPrincipal User student) {
        return ResponseEntity.ok(certificateService.getMyCertificates(student));
    }

    /** Générer le certificat pour un cours (si 100% complété) */
    @PostMapping("/generate/{courseId}")
    public ResponseEntity<CertificateDto> generate(
            @PathVariable Long courseId,
            @AuthenticationPrincipal User student) {
        return ResponseEntity.ok(certificateService.generateCertificate(courseId, student));
    }

    /** Détail d'un certificat (propriétaire uniquement) */
    @GetMapping("/{id}")
    public ResponseEntity<CertificateDto> getCertificate(
            @PathVariable Long id,
            @AuthenticationPrincipal User student) {
        return ResponseEntity.ok(certificateService.getMyCertificate(id, student));
    }
}

