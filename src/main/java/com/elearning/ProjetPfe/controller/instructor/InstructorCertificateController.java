package com.elearning.ProjetPfe.controller.instructor;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.dto.learning.CertificateDto;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.service.learning.CertificateService;

/**
 * Endpoints certificats pour l'instructeur.
 *
 * GET  /api/instructor/certificates  → liste tous les certificats émis pour ses cours
 */
@RestController
@RequestMapping("/api/instructor/certificates")
public class InstructorCertificateController {

    @Autowired
    private CertificateService certificateService;

    @GetMapping
    public ResponseEntity<List<CertificateDto>> getInstructorCertificates(
            @AuthenticationPrincipal User instructor) {
        return ResponseEntity.ok(certificateService.getInstructorCertificates(instructor));
    }
}
