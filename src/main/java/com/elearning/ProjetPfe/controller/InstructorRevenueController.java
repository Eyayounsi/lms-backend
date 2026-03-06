package com.elearning.ProjetPfe.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.elearning.ProjetPfe.dto.InstructorRevenueDto;
import com.elearning.ProjetPfe.entity.User;
import com.elearning.ProjetPfe.service.InstructorRevenueService;

/**
 * Tableau de bord revenus de l'instructor connecté.
 *
 * GET /api/instructor/revenue → dashboard complet
 */
@RestController
@RequestMapping("/api/instructor/revenue")
public class InstructorRevenueController {

    @Autowired
    private InstructorRevenueService revenueService;

    @GetMapping
    public ResponseEntity<InstructorRevenueDto> getMyRevenue(
            @AuthenticationPrincipal User instructor) {
        return ResponseEntity.ok(revenueService.getMyRevenueDashboard(instructor));
    }
}

