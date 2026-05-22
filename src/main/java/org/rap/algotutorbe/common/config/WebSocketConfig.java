package org.rap.algotutorbe.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Kích hoạt Simple Broker tại prefix "/topic"
        // Nghĩa là: Khi Spring Boot gọi messagingTemplate.convertAndSend("/topic/...", payload)
        // thì client lắng nghe prefix này sẽ nhận được.
        config.enableSimpleBroker("/topic");

        // Tiền tố dành cho các message mà Client (Next.js) gửi lên Server (nếu cần)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Định nghĩa endpoint "/ws" để Next.js thực hiện handshake kết nối ban đầu
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:3000", "https://your-production-domain.com") // Cấu hình CORS cực kỳ quan trọng
        // .withSockJS() // Bật dòng này nếu bạn muốn hỗ trợ fallback cho các trình duyệt quá cũ không hỗ trợ chuẩn WebSocket
        ;
    }
}