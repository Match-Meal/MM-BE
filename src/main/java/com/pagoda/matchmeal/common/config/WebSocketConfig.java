package com.pagoda.matchmeal.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 1. 클라이언트 연결 엔드포인트: ws://localhost:8080/ws-stomp
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*") // CORS 허용
                .withSockJS(); // SockJS 지원 (브라우저 호환성)
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 2. 메시지 구독 요청 url (클라이언트가 듣는 곳)
        // /topic: 전체 알림 (공지, 아침 알림)
        // /queue: 개인 알림 (팔로잉, 좋아요)
        registry.enableSimpleBroker("/topic", "/queue");
    }
}