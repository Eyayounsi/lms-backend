package com.elearning.ProjetPfe.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.elearning.ProjetPfe.dto.OrderHistoryDto;
import com.elearning.ProjetPfe.entity.User;
import com.elearning.ProjetPfe.service.OrderService;

/**
 * Historique des commandes de l'étudiant connecté.
 *
 * GET /api/student/orders → liste toutes les commandes
 */
@RestController
@RequestMapping("/api/student/orders")
public class StudentOrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping
    public ResponseEntity<List<OrderHistoryDto>> getMyOrders(
            @AuthenticationPrincipal User student) {
        return ResponseEntity.ok(orderService.getMyOrders(student));
    }
}

