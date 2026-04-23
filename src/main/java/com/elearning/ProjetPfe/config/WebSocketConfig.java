package com.elearning.ProjetPfe.config;

import java.security.Principal;
import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.http.server.ServerHttpRequest;

import com.elearning.ProjetPfe.websocket.JwtHandshakeInterceptor;

/**
 * Configuration WebSocket STOMP pour les notifications temps réel
 * ==========================================
 * 
 * Flux de communication:
 * 1. Client se connecte: GET /ws (SockJS upgrade vers WebSocket)
 * 2. Client subscribe: /user/queue/notifications (messages privés)
 *    ou /topic/admin-notifications (broadcast publique)
 * 3. Serveur push: messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", dto)
 * 4. Client reçoit en temps réel via StompJS + SockJS
 * 
 * Endpoints:
 * - WebSocket: ws://localhost:8081/ws
 * - Fallback HTTP: http://localhost:8081/ws
 * - Message broker: SimpleBroker avec préfixes:
 *    * /user/* pour les messages privés
 *    * /topic/* pour les broadcasts
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    public WebSocketConfig(JwtHandshakeInterceptor jwtHandshakeInterceptor) {
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Message broker: 
        // - /topic/* pour les messages publiques (broadcast)
        // - /queue/* pour les messages privés (point-to-point)
        config.enableSimpleBroker("/topic", "/queue");
        
        // Préfixe pour les messages envoyés par les clients
        config.setApplicationDestinationPrefixes("/app");
        
        // Préfixe pour les messages privés utilisateur
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint WebSocket
        registry.addEndpoint("/ws")
            .addInterceptors(jwtHandshakeInterceptor)
            .setHandshakeHandler(new DefaultHandshakeHandler() {
                @Override
                protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
                    Object userId = attributes.get("userId");
                    if (userId != null) {
                        return userId::toString;
                    }
                    return super.determineUser(request, wsHandler, attributes);
                }
            })
            .setAllowedOriginPatterns("*")  // Permettre CORS
            .withSockJS();                   // Fallback HTTP si WebSocket indisponible
    }
}
