package com.szwl.supportbot.imdemo.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * WebSocket 配置类
 * 配置STOMP消息代理和端点
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setHandshakeHandler(new DefaultHandshakeHandler() {
                    @Override
                    protected Principal determineUser(ServerHttpRequest request,
                                                      WebSocketHandler wsHandler,
                                                      Map<String, Object> attributes) {
                        // 获取用户ID参数
                        String userId = null;
                        if (request instanceof ServletServerHttpRequest) {
                            HttpServletRequest servletRequest =
                                    ((ServletServerHttpRequest) request).getServletRequest();
                            userId = servletRequest.getParameter("uid");
                        }
                        
                        // 将用户ID存储到attributes中，供消息处理使用
                        if (userId != null && !userId.isEmpty()) {
                            attributes.put("userId", userId);
                        }
                        
                        // 使用用户ID作为Principal名称，这样Spring STOMP的用户目标前缀机制就能正确工作
                        // 客户端订阅 /user/queue/messages 会被转换为 /user/{userId}/queue/messages
                        final String principalName = (userId != null && !userId.isEmpty()) ? userId : UUID.randomUUID().toString();
                        return () -> principalName; // Principal.getName() = userId
                    }
                })
                .setAllowedOrigins("http://127.0.0.1:8081", "http://localhost:8081")  // 明确允许前端源
                .withSockJS();  // 启用SockJS支持
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 配置消息代理
        registry.enableSimpleBroker("/topic", "/queue");  // 启用简单内存消息代理
        
        // 设置应用程序目标前缀
        registry.setApplicationDestinationPrefixes("/app");
        
        // 设置用户目标前缀，用于点对点消息
        registry.setUserDestinationPrefix("/user");
    }
}

