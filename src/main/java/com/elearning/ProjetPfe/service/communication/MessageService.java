package com.elearning.ProjetPfe.service.communication;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.elearning.ProjetPfe.entity.auth.User;
import com.elearning.ProjetPfe.entity.communication.Conversation;
import com.elearning.ProjetPfe.entity.communication.Message;
import com.elearning.ProjetPfe.entity.communication.MessageType;
import com.elearning.ProjetPfe.entity.communication.NotificationType;
import com.elearning.ProjetPfe.repository.auth.UserRepository;
import com.elearning.ProjetPfe.repository.communication.ConversationRepository;
import com.elearning.ProjetPfe.repository.communication.MessageRepository;

@Service
public class MessageService {

    @Autowired private ConversationRepository conversationRepository;
    @Autowired private MessageRepository messageRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private MessageRealtimePublisher messageRealtimePublisher;
    @Autowired private NotificationService notificationService;

    // ═══════════════════════════════════════════════════════════════════════
    //  Conversations
    // ═══════════════════════════════════════════════════════════════════════

    /** Liste des conversations de l'utilisateur avec preview */
    public List<Map<String, Object>> getConversations(User user) {
        List<Conversation> conversations = conversationRepository.findByParticipant(user.getId());

        return conversations.stream().map(c -> {
            User other = c.getOtherParticipant(user);
            long unread = messageRepository.countByConversationIdAndReadFalseAndSenderIdNot(c.getId(), user.getId());

            // Dernier message
            List<Message> msgs = messageRepository.findByConversationIdOrderBySentAtAsc(c.getId());
            Message lastMsg = msgs.isEmpty() ? null : msgs.get(msgs.size() - 1);

            Map<String, Object> dto = new LinkedHashMap<>();
            dto.put("conversationId", c.getId());
            dto.put("participantId", other.getId());
            dto.put("participantName", other.getFullName());
            dto.put("participantAvatar", other.getAvatarPath());
            dto.put("participantRole", other.getRole().name());
            dto.put("unreadCount", unread);
            dto.put("lastMessage", lastMsg != null ? lastMsg.getContent() : null);
            dto.put("lastMessageAt", lastMsg != null ? lastMsg.getSentAt() : c.getCreatedAt());
            dto.put("lastMessageIsOwn", lastMsg != null && lastMsg.getSender().getId().equals(user.getId()));
            return dto;
        })
        .sorted(Comparator.<Map<String, Object>, LocalDateTime>comparing(
                m -> (LocalDateTime) m.get("lastMessageAt")).reversed())
        .collect(Collectors.toList());
    }

    /** Obtenir ou créer une conversation avec un autre utilisateur */
    @Transactional
    public Map<String, Object> getOrCreateConversation(Long otherUserId, User user) {
        if (otherUserId.equals(user.getId())) {
            throw new RuntimeException("Impossible de créer une conversation avec soi-même");
        }
        User other = userRepository.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));

        Conversation conversation = conversationRepository.findByParticipants(user.getId(), otherUserId)
                .orElseGet(() -> {
                    Conversation c = new Conversation();
                    c.setParticipant1(user);
                    c.setParticipant2(other);
                    return conversationRepository.save(c);
                });

        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("conversationId", conversation.getId());
        dto.put("participantId", other.getId());
        dto.put("participantName", other.getFullName());
        dto.put("participantAvatar", other.getAvatarPath());
        return dto;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Messages
    // ═══════════════════════════════════════════════════════════════════════

    /** Récupérer les messages d'une conversation */
    @Transactional
    public List<Map<String, Object>> getMessages(Long conversationId, User user) {
        Conversation convo = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation introuvable"));
        if (!convo.getParticipant1().getId().equals(user.getId())
                && !convo.getParticipant2().getId().equals(user.getId())) {
            throw new RuntimeException("Accès non autorisé");
        }

        // Marquer les messages non lus comme lus
        List<Message> unread = messageRepository.findByConversationIdAndReadFalseAndSenderIdNot(conversationId, user.getId());
        for (Message m : unread) {
            m.setRead(true);
            m.setReadAt(LocalDateTime.now());
        }
        messageRepository.saveAll(unread);

        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId)
                .stream().map(this::mapMessageToDto).collect(Collectors.toList());
    }

    /** Envoyer un message */
    @Transactional
    public Map<String, Object> sendMessage(Long conversationId, Map<String, Object> data, User sender) {
        Conversation convo = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation introuvable"));
        if (!convo.getParticipant1().getId().equals(sender.getId())
                && !convo.getParticipant2().getId().equals(sender.getId())) {
            throw new RuntimeException("Accès non autorisé");
        }

        Message message = new Message();
        message.setContent((String) data.get("content"));
        message.setMessageType(data.containsKey("messageType")
                ? MessageType.valueOf((String) data.get("messageType"))
                : MessageType.TEXT);
        message.setSender(sender);
        message.setConversation(convo);

        message = messageRepository.save(message);

        // Mettre à jour lastMessageAt
        convo.setLastMessageAt(message.getSentAt());
        conversationRepository.save(convo);

        Map<String, Object> dto = mapMessageToDto(message);

        // Notifier le destinataire
        User recipient = convo.getParticipant1().getId().equals(sender.getId())
                ? convo.getParticipant2()
                : convo.getParticipant1();
        String preview = message.getContent() != null && message.getContent().length() > 60
                ? message.getContent().substring(0, 60) + "…"
                : message.getContent();
        notificationService.send(
                recipient,
                NotificationType.NEW_MESSAGE,
                "Nouveau message de " + sender.getFullName(),
                preview,
                "/recruiter/messages"
        );

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    messageRealtimePublisher.publishMessageCreated(conversationId, dto);
                }
            });
        } else {
            messageRealtimePublisher.publishMessageCreated(conversationId, dto);
        }

        return dto;
    }

    /** Marquer tous les messages d'une conversation comme lus */
    @Transactional
    public void markAsRead(Long conversationId, User user) {
        Conversation convo = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation introuvable"));
        if (!convo.getParticipant1().getId().equals(user.getId())
                && !convo.getParticipant2().getId().equals(user.getId())) {
            throw new RuntimeException("Accès non autorisé");
        }

        List<Message> unread = messageRepository.findByConversationIdAndReadFalseAndSenderIdNot(conversationId, user.getId());
        for (Message m : unread) {
            m.setRead(true);
            m.setReadAt(LocalDateTime.now());
        }
        messageRepository.saveAll(unread);
    }

    /** Nombre total de messages non lus */
    public Map<String, Object> getTotalUnreadCount(User user) {
        List<Conversation> convos = conversationRepository.findByParticipant(user.getId());
        long total = 0;
        for (Conversation c : convos) {
            total += messageRepository.countByConversationIdAndReadFalseAndSenderIdNot(c.getId(), user.getId());
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("count", total);
        return m;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  Mapper
    // ═══════════════════════════════════════════════════════════════════════

    private Map<String, Object> mapMessageToDto(Message m) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", m.getId());
        dto.put("content", m.getContent());
        dto.put("messageType", m.getMessageType().name());
        dto.put("senderId", m.getSender().getId());
        dto.put("senderName", m.getSender().getFullName());
        dto.put("senderAvatar", m.getSender().getAvatarPath());
        dto.put("isRead", m.isRead());
        dto.put("sentAt", m.getSentAt());
        dto.put("readAt", m.getReadAt());
        return dto;
    }
}
