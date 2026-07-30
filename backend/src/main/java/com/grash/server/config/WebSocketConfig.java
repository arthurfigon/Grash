package com.grash.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final CorsProperties corsProperties;

    public WebSocketConfig(CorsProperties corsProperties) {
        this.corsProperties = corsProperties;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(corsProperties.getAllowedOrigins().toArray(new String[0]))
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // /topic: broadcasts servidor -> sala inteira (estado da sala, estado público da rodada)
        // /queue: usado só junto com /user (abaixo) para a carta privada de cada jogador
        registry.enableSimpleBroker("/topic", "/queue");
        // /app: prefixo para mensagens cliente -> @MessageMapping do servidor
        registry.setApplicationDestinationPrefixes("/app");
        // /user: prefixo pra mensagens endereçadas a uma sessão específica (sem exigir login —
        // ver GameService.sendPrivate, que usa o sessionId no lugar de um usuário autenticado)
        registry.setUserDestinationPrefix("/user");
    }
}
