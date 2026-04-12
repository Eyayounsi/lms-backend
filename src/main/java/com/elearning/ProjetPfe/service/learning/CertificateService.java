package com.elearning.ProjetPfe.service.learning;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.elearning.ProjetPfe.dto.learning.CertificateDto;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.entity.communication.NotificationType;
import com.elearning.ProjetPfe.entity.learning.Certificate;
import com.elearning.ProjetPfe.entity.payment.PaymentStatus;
import com.elearning.ProjetPfe.repository.learning.CertificateRepository;
import com.elearning.ProjetPfe.repository.learning.CourseProgressRepository;
import com.elearning.ProjetPfe.repository.payment.EnrollmentRepository;
import com.elearning.ProjetPfe.service.communication.NotificationService;

/**
 * Service de gestion des certificats.
 *
 * Règles métier :
 *  1. L'étudiant doit avoir complété 100% du cours
 *  2. L'étudiant doit avoir un enrollment PAID (pas remboursé)
 *  3. Un seul certificat par (étudiant, cours)
 *  4. Code unique sécurisé (UUID)
 *  5. Déclenche une notification à l'étudiant
 */
@Service
public class CertificateService {

    @Autowired
    private CertificateRepository certificateRepository;

    @Autowired
    private CourseProgressRepository courseProgressRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private NotificationService notificationService;

    // ═══════════════════════════════════════════════════════════════════════
    //  GÉNÉRER un certificat (appelé automatiquement quand 100% atteint)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public CertificateDto generateCertificate(Long courseId, User student) {

        // 1. Vérifier que l'étudiant a payé (pas de remboursement)
        var enrollment = enrollmentRepository
                .findByStudentIdAndCourseIdAndPaymentStatus(student.getId(), courseId, PaymentStatus.PAID)
                .orElseThrow(() -> new RuntimeException(
                        "Aucun paiement confirmé trouvé pour ce cours. Certificat non disponible."));

        // 2. Vérifier que la progression est à 100%
        var progress = courseProgressRepository
                .findByStudentIdAndCourseId(student.getId(), courseId)
                .orElseThrow(() -> new RuntimeException("Progression non trouvée pour ce cours."));

        if (progress.getCompletionPercentage() < 100.0) {
            throw new RuntimeException(
                    "Vous devez compléter 100% du cours pour obtenir votre certificat. " +
                    "Progression actuelle : " + progress.getCompletionPercentage() + "%");
        }

        // 3. Vérifier s'il n'existe pas déjà
        var existing = certificateRepository.findByStudentIdAndCourseId(student.getId(), courseId);
        if (existing.isPresent()) {
            return toDto(existing.get());
        }

        // 4. Générer un code unique
        String code = UUID.randomUUID().toString().replace("-", "").toUpperCase();

        // 5. Créer et sauvegarder le certificat
        Certificate cert = new Certificate();
        cert.setStudent(student);
        cert.setCourse(enrollment.getCourse());
        cert.setCertificateCode(code);
        cert = certificateRepository.save(cert);

        // 6. Déclencher la notification
        notificationService.send(
                student,
                NotificationType.CERTIFICATE_ISSUED,
                "🎓 Certificat obtenu !",
                "Félicitations ! Vous avez obtenu votre certificat pour le cours \"" +
                        enrollment.getCourse().getTitle() + "\".",
                "/student/certificates"
        );

        return toDto(cert);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TÉLÉCHARGER (retourner les infos) — seul le propriétaire
    // ═══════════════════════════════════════════════════════════════════════

    public CertificateDto getMyCertificate(Long certId, User student) {
        Certificate cert = certificateRepository.findById(certId)
                .orElseThrow(() -> new RuntimeException("Certificat non trouvé"));

        // Sécurité : seul le propriétaire peut accéder
        if (!cert.getStudent().getId().equals(student.getId())) {
            throw new RuntimeException("Accès non autorisé à ce certificat");
        }

        return toDto(cert);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  LISTE des certificats d'un étudiant
    // ═══════════════════════════════════════════════════════════════════════

    public List<CertificateDto> getMyCertificates(User student) {
        return certificateRepository
                .findByStudentIdOrderByIssuedAtDesc(student.getId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  LISTE des certificats émis pour les cours d'un instructeur
    // ═══════════════════════════════════════════════════════════════════════

    public List<CertificateDto> getInstructorCertificates(User instructor) {
        return certificateRepository
                .findByCourse_InstructorIdOrderByIssuedAtDesc(instructor.getId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  VÉRIFICATION PUBLIQUE par code (sans infos sensibles)
    // ═══════════════════════════════════════════════════════════════════════

    public CertificateDto verifyByCode(String code) {
        Certificate cert = certificateRepository.findByCertificateCode(code)
                .orElseThrow(() -> new RuntimeException("Certificat introuvable ou code invalide"));

        // DTO public : on ne retourne pas l'ID étudiant (privacy)
        CertificateDto dto = new CertificateDto();
        dto.setId(cert.getId());
        dto.setCourseId(cert.getCourse().getId());
        dto.setCourseTitle(cert.getCourse().getTitle());
        dto.setStudentName(cert.getStudent().getFullName());
        dto.setStudentAvatar(cert.getStudent().getAvatarPath());
        dto.setInstructorName(cert.getCourse().getInstructor().getFullName());
        dto.setCertificateCode(cert.getCertificateCode());
        dto.setIssuedAt(cert.getIssuedAt());
        // On ne retourne PAS studentId pour la vérification publique
        return dto;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CONVERSION
    // ═══════════════════════════════════════════════════════════════════════

    private CertificateDto toDto(Certificate cert) {
        CertificateDto dto = new CertificateDto();
        dto.setId(cert.getId());
        dto.setStudentId(cert.getStudent().getId());
        dto.setStudentName(cert.getStudent().getFullName());
        dto.setStudentAvatar(cert.getStudent().getAvatarPath());
        dto.setCourseId(cert.getCourse().getId());
        dto.setCourseTitle(cert.getCourse().getTitle());
        dto.setInstructorName(cert.getCourse().getInstructor().getFullName());
        dto.setCertificateCode(cert.getCertificateCode());
        dto.setIssuedAt(cert.getIssuedAt());
        return dto;
    }
}

