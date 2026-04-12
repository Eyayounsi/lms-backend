package com.elearning.ProjetPfe.controller.recruiter;

import com.elearning.ProjetPfe.entity.communication.Message;
import com.elearning.ProjetPfe.entity.communication.Conversation;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.entity.learning.Certificate;
import com.elearning.ProjetPfe.entity.recruiter.JobOffer;
import com.elearning.ProjetPfe.entity.recruiter.JobOfferStatus;
import com.elearning.ProjetPfe.entity.recruiter.JobOfferType;
import com.elearning.ProjetPfe.entity.auth.Role;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.learning.CertificateRepository;
import com.elearning.ProjetPfe.repository.recruiter.JobOfferRepository;
import com.elearning.ProjetPfe.repository.auth.UserRepository;
import com.elearning.ProjetPfe.service.communication.MessageService;

/**
 * Endpoints RECRUITER — accessibles uniquement avec le rôle RECRUITER.
 *
 * GET    /api/recruiter/stats                            → statistiques du tableau de bord
 * GET    /api/recruiter/shared-profiles                  → profils étudiants partagés
 * GET    /api/recruiter/certified-students               → étudiants certifiés (avec recherche)
 * GET    /api/recruiter/job-offers                       → offres du recruteur
 * POST   /api/recruiter/job-offers                       → créer une offre
 * PUT    /api/recruiter/job-offers/{id}                  → modifier une offre
 * DELETE /api/recruiter/job-offers/{id}                  → supprimer une offre
 * GET    /api/recruiter/conversations                    → liste des conversations
 * POST   /api/recruiter/conversations/{studentId}        → démarrer/récupérer une conversation
 * GET    /api/recruiter/conversations/{id}/messages      → messages d'une conversation
 * POST   /api/recruiter/conversations/{id}/messages      → envoyer un message
 */
@RestController
@RequestMapping("/api/recruiter")
public class RecruiterController {

    private final UserRepository userRepository;
    private final CertificateRepository certificateRepository;
    private final JobOfferRepository jobOfferRepository;
    private final MessageService messageService;

    public RecruiterController(UserRepository userRepository,
                                CertificateRepository certificateRepository,
                                JobOfferRepository jobOfferRepository,
                                MessageService messageService) {
        this.userRepository = userRepository;
        this.certificateRepository = certificateRepository;
        this.jobOfferRepository = jobOfferRepository;
        this.messageService = messageService;
    }


    // ─── Statistiques tableau de bord ────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @AuthenticationPrincipal User currentUser) {

        long sharedProfiles = userRepository
                .findByRoleAndShareWithRecruiters(Role.STUDENT, true)
                .size();
        long totalStudents = userRepository.countByRole(Role.STUDENT);
        long totalOffers = jobOfferRepository.countByRecruiterId(currentUser.getId());

        Map<String, Object> unreadMap = messageService.getTotalUnreadCount(currentUser);
        long unreadMessages = ((Number) unreadMap.get("count")).longValue();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("sharedProfiles", sharedProfiles);
        stats.put("totalStudents", totalStudents);
        stats.put("pendingOffers", totalOffers);
        stats.put("scheduledInterviews", unreadMessages);

        return ResponseEntity.ok(stats);
    }

    // ─── Profils étudiants partagés ──────────────────────────────────────────

    @GetMapping("/shared-profiles")
    public ResponseEntity<List<Map<String, Object>>> getSharedProfiles(
            @AuthenticationPrincipal User currentUser) {

        List<User> students = userRepository
                .findByRoleAndShareWithRecruiters(Role.STUDENT, true);

        List<Map<String, Object>> profiles = new ArrayList<>();
        for (User s : students) {
            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("id", s.getId());
            profile.put("fullName", s.getFullName());
            profile.put("email", s.getEmail());
            profile.put("avatarPath", s.getAvatarPath());
            profile.put("bio", s.getBio());
            profile.put("designation", s.getDesignation());
            profile.put("linkedinUrl", s.getLinkedinUrl());
            profile.put("educationJson", s.getEducationJson());
            profile.put("experienceJson", s.getExperienceJson());
            profiles.add(profile);
        }

        return ResponseEntity.ok(profiles);
    }

    // ─── Étudiants certifiés ─────────────────────────────────────────────────

    /**
     * Retourne les étudiants ayant au moins un certificat et ayant activé
     * le partage de profil. La recherche filtre par nom, email, désignation
     * ou titre de cours.
     */
    @GetMapping("/certified-students")
    public ResponseEntity<List<Map<String, Object>>> getCertifiedStudents(
            @RequestParam(required = false, defaultValue = "") String search,
            @AuthenticationPrincipal User currentUser) {

        List<User> students = certificateRepository.findStudentsWithCertificatesAndSharedProfile();
        String q = search.toLowerCase().trim();

        List<Map<String, Object>> result = new ArrayList<>();
        for (User s : students) {
            List<Certificate> certs = certificateRepository.findByStudentIdOrderByIssuedAtDesc(s.getId());

            if (!q.isEmpty()) {
                boolean studentMatch = (s.getFullName() != null && s.getFullName().toLowerCase().contains(q))
                        || (s.getEmail() != null && s.getEmail().toLowerCase().contains(q))
                        || (s.getDesignation() != null && s.getDesignation().toLowerCase().contains(q));
                boolean certMatch = certs.stream().anyMatch(c ->
                        c.getCourse() != null && c.getCourse().getTitle().toLowerCase().contains(q));
                if (!studentMatch && !certMatch) continue;
            }

            List<Map<String, Object>> certDtos = new ArrayList<>();
            for (Certificate cert : certs) {
                Map<String, Object> certDto = new LinkedHashMap<>();
                certDto.put("id", cert.getId());
                certDto.put("courseName", cert.getCourse() != null ? cert.getCourse().getTitle() : "");
                certDto.put("certificateCode", cert.getCertificateCode());
                certDto.put("issuedAt", cert.getIssuedAt());
                certDtos.add(certDto);
            }

            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", s.getId());
            dto.put("fullName", s.getFullName());
            dto.put("email", s.getEmail());
            dto.put("avatarPath", s.getAvatarPath());
            dto.put("bio", s.getBio());
            dto.put("designation", s.getDesignation());
            dto.put("linkedinUrl", s.getLinkedinUrl());
            dto.put("certificates", certDtos);
            result.add(dto);
        }

        return ResponseEntity.ok(result);
    }

    // ─── Offres d'emploi ─────────────────────────────────────────────────────

    @GetMapping("/job-offers")
    public ResponseEntity<List<Map<String, Object>>> getJobOffers(
            @AuthenticationPrincipal User currentUser) {
        List<JobOffer> offers = jobOfferRepository.findByRecruiterIdOrderByCreatedAtDesc(currentUser.getId());
        return ResponseEntity.ok(offers.stream().map(this::mapOffer).collect(Collectors.toList()));
    }

    @PostMapping("/job-offers")
    public ResponseEntity<Map<String, Object>> createJobOffer(
            @RequestBody Map<String, Object> data,
            @AuthenticationPrincipal User currentUser) {
        JobOffer offer = buildOffer(data, new JobOffer(), currentUser);
        offer = jobOfferRepository.save(offer);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapOffer(offer));
    }

    @PutMapping("/job-offers/{id}")
    public ResponseEntity<Map<String, Object>> updateJobOffer(
            @PathVariable Long id,
            @RequestBody Map<String, Object> data,
            @AuthenticationPrincipal User currentUser) {
        JobOffer offer = jobOfferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offre introuvable"));
        if (!offer.getRecruiter().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        offer = buildOffer(data, offer, currentUser);
        offer = jobOfferRepository.save(offer);
        return ResponseEntity.ok(mapOffer(offer));
    }

    @DeleteMapping("/job-offers/{id}")
    public ResponseEntity<Void> deleteJobOffer(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        JobOffer offer = jobOfferRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offre introuvable"));
        if (!offer.getRecruiter().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        jobOfferRepository.delete(offer);
        return ResponseEntity.noContent().build();
    }

    // ─── Messagerie ──────────────────────────────────────────────────────────

    @GetMapping("/conversations")
    public ResponseEntity<List<Map<String, Object>>> getConversations(
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(messageService.getConversations(currentUser));
    }

    @PostMapping("/conversations/{studentId}")
    public ResponseEntity<Map<String, Object>> getOrCreateConversation(
            @PathVariable Long studentId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(messageService.getOrCreateConversation(studentId, currentUser));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<Map<String, Object>>> getMessages(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(messageService.getMessages(conversationId, currentUser));
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody Map<String, Object> data,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(messageService.sendMessage(conversationId, data, currentUser));
    }

    // ─── Helpers privés ──────────────────────────────────────────────────────

    private Map<String, Object> mapOffer(JobOffer o) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", o.getId());
        dto.put("title", o.getTitle());
        dto.put("description", o.getDescription());
        dto.put("offerType", o.getOfferType().name());
        dto.put("location", o.getLocation());
        dto.put("contractType", o.getContractType());
        dto.put("requiredSkills", o.getRequiredSkills());
        dto.put("deadline", o.getDeadline());
        dto.put("status", o.getStatus().name());
        dto.put("createdAt", o.getCreatedAt());
        dto.put("updatedAt", o.getUpdatedAt());
        return dto;
    }

    private JobOffer buildOffer(Map<String, Object> data, JobOffer offer, User recruiter) {
        if (data.containsKey("title")) offer.setTitle((String) data.get("title"));
        if (data.containsKey("description")) offer.setDescription((String) data.get("description"));
        if (data.containsKey("offerType")) offer.setOfferType(JobOfferType.valueOf((String) data.get("offerType")));
        if (data.containsKey("location")) offer.setLocation((String) data.get("location"));
        if (data.containsKey("contractType")) offer.setContractType((String) data.get("contractType"));
        if (data.containsKey("requiredSkills")) offer.setRequiredSkills((String) data.get("requiredSkills"));
        if (data.containsKey("deadline") && data.get("deadline") != null) {
            offer.setDeadline(LocalDate.parse((String) data.get("deadline")));
        }
        if (data.containsKey("status")) offer.setStatus(JobOfferStatus.valueOf((String) data.get("status")));
        offer.setRecruiter(recruiter);
        return offer;
    }
}
