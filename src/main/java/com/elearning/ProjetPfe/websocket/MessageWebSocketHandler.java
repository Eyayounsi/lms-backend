package com.elearning.ProjetPfe.websocket;

import com.elearning.ProjetPfe.entity.communication.Conversation;
import com.elearning.ProjetPfe.repository.communication.ConversationRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MessageWebSocketHandler extends TextWebSocketHandler {

    private final ConversationRepository conversationRepository;
    private final ObjectMapper objectMapper;

    private final Map<String, Long> sessionUsers = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> conversationSubscribers = new ConcurrentHashMap<>();

    public MessageWebSocketHandler(ConversationRepository conversationRepository, ObjectMapper objectMapper) {
        this.conversationRepository = conversationRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Object userIdAttr = session.getAttributes().get("userId");
        if (!(userIdAttr instanceof Long userId)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unauthorized"));
            return;
        }

        sessions.put(session.getId(), session);
        sessionUsers.put(session.getId(), userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonNode payload;
        try {
            payload = objectMapper.readTree(message.getPayload());
        } catch (Exception e) {
            return;
        }

        String type = payload.path("type").asText("");
        long conversationId = payload.path("conversationId").asLong(-1L);

        if (conversationId <= 0) {
            return;
        }

        if ("SUBSCRIBE".equalsIgnoreCase(type)) {
            subscribeToConversation(session, conversationId);
        } else if ("UNSUBSCRIBE".equalsIgnoreCase(type)) {
            unsubscribeFromConversation(session, conversationId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        cleanupSession(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        cleanupSession(session.getId());
    }

    public void broadcastMessageCreated(Long conversationId, Map<String, Object> messageDto) {
        Set<String> subscribers = conversationSubscribers.get(conversationId);
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }

        Map<String, Object> payload = Map.of(
                "type", "MESSAGE_CREATED",
                "conversationId", conversationId,
                "message", messageDto
        );

        try {
            String json = objectMapper.writeValueAsString(payload);
            TextMessage textMessage = new TextMessage(json);
            for (String sessionId : subscribers) {
                WebSocketSession session = sessions.get(sessionId);
                if (session != null && session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private void subscribeToConversation(WebSocketSession session, long conversationId) {
        Long userId = sessionUsers.get(session.getId());
        if (userId == null || !userCanAccessConversation(userId, conversationId)) {
            return;
        }

        conversationSubscribers
                .computeIfAbsent(conversationId, ignored -> ConcurrentHashMap.newKeySet())
                .add(session.getId());
    }

    private void unsubscribeFromConversation(WebSocketSession session, long conversationId) {
        Set<String> subscribers = conversationSubscribers.get(conversationId);
        if (subscribers == null) {
            return;
        }

        subscribers.remove(session.getId());
        if (subscribers.isEmpty()) {
            conversationSubscribers.remove(conversationId);
        }
    }

    private boolean userCanAccessConversation(Long userId, long conversationId) {
        Conversation convo = conversationRepository.findById(conversationId).orElse(null);
        if (convo == null) {
            return false;
        }

        Long p1 = convo.getParticipant1() != null ? convo.getParticipant1().getId() : null;
        Long p2 = convo.getParticipant2() != null ? convo.getParticipant2().getId() : null;
        return userId.equals(p1) || userId.equals(p2);
    }

    private void cleanupSession(String sessionId) {
        sessions.remove(sessionId);
        sessionUsers.remove(sessionId);
        conversationSubscribers.values().forEach(set -> set.remove(sessionId));
        conversationSubscribers.entrySet().removeIf(e -> e.getValue().isEmpty());
    }
}
