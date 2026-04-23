package com.elearning.ProjetPfe.controller.communication;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import com.elearning.ProjetPfe.dto.communication.NotificationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocket pour les notifications temps réel
 * ================================================
 * 
 * Flux:
 * 1. Frontend se connecte via SockJS/STOMP: /ws/notifications/{userId}
 * 2. Backend push une notification: messaging.convertAndSendToUser(userId, "/queue/notifications", dto)
 * 3. Frontend reçoit et affiche un toast
 * 
 * Endpoints:
 * - Frontend: ws://localhost:8081/ws (connecter via SockJS)
 * - Subscribe: /user/queue/notifications (recevoir les notifications)
 */
@Controller
public class NotificationWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketController.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * Envoyer une notification à un utilisateur spécifique
     * Appelé par NotificationService.send()
     */
    public void sendNotificationToUser(Long userId, NotificationDto dto) {
        try {
            // Envoyer via WebSocket
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                dto
            );
            log.info("[WS] Notification envoyée à l'utilisateur {} : {}", userId, dto.getTitle());
        } catch (Exception e) {
            log.error("[WS] Erreur envoi notification utilisateur {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Envoyer une notification broadcast à tous les admins
     */
    public void broadcastToAdmins(NotificationDto dto) {
        try {
            // Broadcast via l'endpoint /topic/admin-notifications
            messagingTemplate.convertAndSend("/topic/admin-notifications", dto);
            log.info("[WS] Broadcast admin: {}", dto.getTitle());
        } catch (Exception e) {
            log.error("[WS] Erreur broadcast admins: {}", e.getMessage());
        }
    }

    /**
     * Health check: frontend peut envoyer un ping
     */
    @MessageMapping("/ping")
    @SendTo("/topic/pong")
    public Map<String, Object> ping() {
        return Map.of("status", "pong", "timestamp", System.currentTimeMillis());
    }
}

