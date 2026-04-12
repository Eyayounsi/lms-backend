package com.elearning.ProjetPfe.service.communication;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elearning.ProjetPfe.dto.communication.NotificationDto;
import com.elearning.ProjetPfe.entity.communication.Notification;
import com.elearning.ProjetPfe.entity.communication.NotificationType;
import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.communication.NotificationRepository;
import com.elearning.ProjetPfe.controller.communication.NotificationWebSocketController;

/**
 * Service centralisé de notifications internes.
 *
 * Utilisé par :
 *   - PaymentService    → PURCHASE_CONFIRMED, NEW_REVENUE
 *   - CourseService     → COURSE_APPROVED, COURSE_REJECTED
 *   - CertificateService → CERTIFICATE_ISSUED
 *
 * Règle sécurité : un utilisateur ne voit QUE ses notifications.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired(required = false)
    private NotificationWebSocketController webSocketController;

    // ═══════════════════════════════════════════════════════════════════════
    //  CRÉER une notification (appelé par les autres services)
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public void send(User user, NotificationType type, String title, String message, String link) {
        Notification notif = new Notification(user, type, title, message, link);
        notificationRepository.save(notif);

        // Envoyer via WebSocket en temps réel
        try {
            if (webSocketController != null) {
                NotificationDto dto = toDto(notif);
                webSocketController.sendNotificationToUser(user.getId(), dto);
            }
        } catch (Exception e) {
            log.warn("[NOTIF] Erreur envoi WebSocket pour l'utilisateur {}: {}", user.getId(), e.getMessage());
            // Ne pas échouer si WebSocket plante (fallback HTTP polling toujours disponible)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  LIRE les notifications de l'utilisateur connecté
    // ═══════════════════════════════════════════════════════════════════════

    public List<NotificationDto> getMyNotifications(User user) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  COMPTER les non lues
    // ═══════════════════════════════════════════════════════════════════════

    public long countUnread(User user) {
        return notificationRepository.countByUserIdAndReadFalse(user.getId());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  MARQUER UNE notification comme lue
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public NotificationDto markAsRead(Long notifId, User user) {
        Notification notif = notificationRepository.findById(notifId)
                .orElseThrow(() -> new RuntimeException("Notification non trouvée"));

        // Sécurité : seul le propriétaire peut marquer
        if (!notif.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Accès non autorisé");
        }

        notif.setRead(true);
        return toDto(notificationRepository.save(notif));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  MARQUER TOUTES comme lues
    // ═══════════════════════════════════════════════════════════════════════

    @Transactional
    public void markAllAsRead(User user) {
        List<Notification> unread = notificationRepository.findByUserIdAndReadFalse(user.getId());
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CONVERSION
    // ═══════════════════════════════════════════════════════════════════════

    public NotificationDto toDto(Notification n) {
        NotificationDto dto = new NotificationDto();
        dto.setId(n.getId());
        dto.setType(n.getType().name());
        dto.setTitle(n.getTitle());
        dto.setMessage(n.getMessage());
        dto.setLink(n.getLink());
        dto.setRead(n.isRead());
        dto.setCreatedAt(n.getCreatedAt());
        return dto;
    }
}

