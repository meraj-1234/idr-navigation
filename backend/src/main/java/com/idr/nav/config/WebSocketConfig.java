package com.idr.nav.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket & STOMP configuration for broadcasting live telemetry,
 * GNSS status alerts, and navigation events to the frontend.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // In-memory message broker for broadcasting to subscribers
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Standard WebSocket endpoint & SockJS fallback
        registry.addEndpoint("/ws-telemetry")
                .setAllowedOriginPatterns("*");
        registry.addEndpoint("/ws-telemetry")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
