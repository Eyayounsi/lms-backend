package com.elearning.ProjetPfe.service.communication;

import com.elearning.ProjetPfe.websocket.MessageWebSocketHandler;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MessageRealtimePublisher {

    private final MessageWebSocketHandler messageWebSocketHandler;

    public MessageRealtimePublisher(MessageWebSocketHandler messageWebSocketHandler) {
        this.messageWebSocketHandler = messageWebSocketHandler;
    }

    public void publishMessageCreated(Long conversationId, Map<String, Object> messageDto) {
        messageWebSocketHandler.broadcastMessageCreated(conversationId, messageDto);
    }
}
