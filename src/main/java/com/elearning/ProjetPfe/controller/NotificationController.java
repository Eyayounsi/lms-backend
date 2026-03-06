package com.elearning.ProjetPfe.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.elearning.ProjetPfe.dto.NotificationDto;
import com.elearning.ProjetPfe.entity.User;
import com.elearning.ProjetPfe.service.NotificationService;

/**
 * Endpoints notifications pour l'utilisateur connecté.
 *
 * GET  /api/notifications              → liste toutes mes notifications
 * GET  /api/notifications/unread-count → nombre de non lues
 * PUT  /api/notifications/{id}/read    → marquer une notification comme lue
 * PUT  /api/notifications/read-all     → marquer toutes comme lues
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /** Toutes les notifications de l'utilisateur connecté */
    @GetMapping
    public ResponseEntity<List<NotificationDto>> getMyNotifications(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.getMyNotifications(user));
    }

    /** Nombre de notifications non lues (pour le badge de la cloche) */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("count", notificationService.countUnread(user)));
    }

    /** Marquer une notification comme lue */
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationDto> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(notificationService.markAsRead(id, user));
    }

    /** Marquer toutes les notifications comme lues */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal User user) {
        notificationService.markAllAsRead(user);
        return ResponseEntity.noContent().build();
    }
}

