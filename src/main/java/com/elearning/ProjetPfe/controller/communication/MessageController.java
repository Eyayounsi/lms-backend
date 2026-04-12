package com.elearning.ProjetPfe.controller.communication;

import com.elearning.ProjetPfe.entity.communication.Message;
import com.elearning.ProjetPfe.entity.communication.Conversation;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.repository.auth.UserRepository;
import com.elearning.ProjetPfe.service.communication.MessageService;

/**
 * Endpoints messagerie — accessible par tout utilisateur authentifié.
 */
@RestController
@RequestMapping("/api/user/messages")
public class MessageController {

    @Autowired private MessageService messageService;
    @Autowired private UserRepository userRepository;

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }

    /** GET /api/user/messages/conversations — Liste des conversations */
    @GetMapping("/conversations")
    public ResponseEntity<List<Map<String, Object>>> getConversations(Authentication auth) {
        return ResponseEntity.ok(messageService.getConversations(getUser(auth)));
    }

    /** POST /api/user/messages/conversations/{otherUserId} — Obtenir ou créer conversation */
    @PostMapping("/conversations/{otherUserId}")
    public ResponseEntity<Map<String, Object>> getOrCreateConversation(
            @PathVariable Long otherUserId, Authentication auth) {
        return ResponseEntity.ok(messageService.getOrCreateConversation(otherUserId, getUser(auth)));
    }

    /** GET /api/user/messages/conversations/{conversationId}/messages — Messages d'une conversation */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<List<Map<String, Object>>> getMessages(
            @PathVariable Long conversationId, Authentication auth) {
        return ResponseEntity.ok(messageService.getMessages(conversationId, getUser(auth)));
    }

    /** POST /api/user/messages/conversations/{conversationId}/messages — Envoyer un message */
    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody Map<String, Object> data, Authentication auth) {
        return ResponseEntity.ok(messageService.sendMessage(conversationId, data, getUser(auth)));
    }

    /** PUT /api/user/messages/conversations/{conversationId}/read — Marquer comme lu */
    @PutMapping("/conversations/{conversationId}/read")
    public ResponseEntity<String> markAsRead(
            @PathVariable Long conversationId, Authentication auth) {
        messageService.markAsRead(conversationId, getUser(auth));
        return ResponseEntity.ok("Messages marqués comme lus");
    }

    /** GET /api/user/messages/unread-count — Nombre total de messages non lus */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> getUnreadCount(Authentication auth) {
        return ResponseEntity.ok(messageService.getTotalUnreadCount(getUser(auth)));
    }
}
