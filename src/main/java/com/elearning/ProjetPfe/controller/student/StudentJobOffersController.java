package com.elearning.ProjetPfe.controller.student;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.elearning.ProjetPfe.entity.recruiter.JobOffer;
import com.elearning.ProjetPfe.entity.recruiter.JobOfferStatus;
import com.elearning.ProjetPfe.repository.recruiter.JobOfferRepository;

/**
 * Endpoint accessible à tous les utilisateurs authentifiés (étudiants inclus).
 * Retourne toutes les offres actives publiées par les recruteurs.
 */
@RestController
@RequestMapping("/api/student")
public class StudentJobOffersController {

    private final JobOfferRepository jobOfferRepository;

    public StudentJobOffersController(JobOfferRepository jobOfferRepository) {
        this.jobOfferRepository = jobOfferRepository;
    }

    /**
     * GET /api/student/job-offers
     * Retourne toutes les offres ACTIVE, avec filtre optionnel par type (EMPLOI|STAGE).
     */
    @GetMapping("/job-offers")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> getActiveJobOffers(
            @RequestParam(required = false) String type) {

        List<JobOffer> offers = jobOfferRepository.findByStatusOrderByCreatedAtDesc(JobOfferStatus.ACTIVE);

        if (type != null && !type.isBlank()) {
            offers = offers.stream()
                    .filter(o -> o.getOfferType().name().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (JobOffer o : offers) {
            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("id", o.getId());
            dto.put("title", o.getTitle());
            dto.put("description", o.getDescription());
            dto.put("offerType", o.getOfferType().name());
            dto.put("location", o.getLocation());
            dto.put("contractType", o.getContractType());
            dto.put("requiredSkills", o.getRequiredSkills());
            dto.put("deadline", o.getDeadline());
            dto.put("createdAt", o.getCreatedAt());
            // Informations recruteur (accès sécurisé dans la transaction)
            if (o.getRecruiter() != null) {
                dto.put("recruiterName", o.getRecruiter().getFullName());
                dto.put("recruiterId", o.getRecruiter().getId());
                dto.put("recruiterEmail", o.getRecruiter().getEmail());
                dto.put("recruiterPhone", o.getRecruiter().getPhone());
                dto.put("companyName", o.getRecruiter().getCompanyName());
            }
            result.add(dto);
        }
        return ResponseEntity.ok(result);
    }
}
