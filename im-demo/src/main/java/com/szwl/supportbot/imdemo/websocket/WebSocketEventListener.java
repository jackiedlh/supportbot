package com.szwl.supportbot.imdemo.websocket;

import com.szwl.supportbot.imdemo.service.UserSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.net.InetSocketAddress;

/**
 * WebSocket 连接事件监听器
 * 处理连接和断开事件
 */
@Slf4j
@Component
public class WebSocketEventListener {

    @Autowired
    private UserSessionService userSessionService;

    /**
     * 处理WebSocket连接事件
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        log.info("WebSocket连接建立: sessionId={}", sessionId);
        
        // 可以在这里添加连接后的初始化逻辑
    }

    /**
     * 处理WebSocket断开事件
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        log.info("WebSocket连接断开: sessionId={}", sessionId);
        
        // 清理用户会话
        userSessionService.removeUserBySessionId(sessionId);
    }
}
