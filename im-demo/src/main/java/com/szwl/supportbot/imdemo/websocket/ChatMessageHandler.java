package com.szwl.supportbot.imdemo.websocket;

import com.szwl.supportbot.imdemo.model.ChatMessage;
import com.szwl.supportbot.imdemo.model.ChatResponse;
import com.szwl.supportbot.imdemo.service.ChatService;
import com.szwl.supportbot.imdemo.service.UserSessionService;
import com.szwl.supportbot.imdemo.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.net.InetSocketAddress;

/**
 * WebSocket 消息处理器
 * 处理用户聊天消息
 */
@Slf4j
@Controller
public class ChatMessageHandler {

    @Autowired
    private ChatService chatService;
    
    @Autowired
    private UserSessionService userSessionService;
    
    @Autowired
    private UserService userService;

    /**
     * 处理用户聊天消息
     */
    @MessageMapping("/chat")
    public void handleChatMessage(@Payload ChatMessage message, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        Long userId = message.getSender();
        
        log.info("=== 收到聊天消息 ===");
        log.info("消息类型: {}", message.getType());
        log.info("发送者: {}", message.getSender());
        log.info("消息内容: {}", message.getContent());
        log.info("时间戳: {}", message.getTimestamp());
        log.info("会话ID: {}", sessionId);
        log.info("连接头信息: {}", headerAccessor.toNativeHeaderMap());
        
        // 获取客户端连接信息
        String clientIp = getClientIp(headerAccessor);
        Integer clientPort = getClientPort(headerAccessor);
        
        // 获取真实用户名
        String username = userService.getUsername(userId);
        if (username == null) {
            log.warn("未找到用户ID对应的用户名: userId={}", userId);
            username = "用户" + userId; // 使用默认用户名
        }
        
        // 建立用户会话
        userSessionService.registerUser(userId, username, sessionId, clientIp, clientPort);
        log.info("用户会话已建立: userId={}, sessionId={}, username={}", userId, sessionId, username);
        
        // 转发给聊天服务处理
        boolean success = chatService.handleUserMessage(message, sessionId);
        
        // 根据处理结果发送响应到用户队列
        ChatResponse response;
        if (success) {
            response = ChatResponse.success(message.getMessageId(), "消息已收到，正在处理中...");
            log.info("消息处理成功: userId={}, messageId={}", userId, message.getMessageId());
        } else {
            response = ChatResponse.failure(message.getMessageId(), "消息处理失败，请重试");
            log.error("消息处理失败: userId={}, messageId={}", userId, message.getMessageId());
        }
        
        // 通过WebSocket发送响应到用户队列
        boolean sent = userSessionService.sendMessageToUser(userId, response);
        log.info("响应消息发送结果: userId={}, messageId={}, sent={}", userId, message.getMessageId(), sent);
        
        log.info("=== 聊天消息处理完成 ===");
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIp(SimpMessageHeaderAccessor headerAccessor) {
        try {
            // 尝试从WebSocket会话中获取远程地址
            Object remoteAddress = headerAccessor.getSessionAttributes().get("remoteAddress");
            if (remoteAddress instanceof InetSocketAddress) {
                return ((InetSocketAddress) remoteAddress).getAddress().getHostAddress();
            }
        } catch (Exception e) {
            log.debug("无法获取客户端IP，使用默认值", e);
        }
        return "127.0.0.1"; // 默认IP
    }
    
    /**
     * 获取客户端端口
     */
    private Integer getClientPort(SimpMessageHeaderAccessor headerAccessor) {
        try {
            // 尝试从WebSocket会话中获取远程地址
            Object remoteAddress = headerAccessor.getSessionAttributes().get("remoteAddress");
            if (remoteAddress instanceof InetSocketAddress) {
                int port = ((InetSocketAddress) remoteAddress).getPort();
                if (port > 0) {
                    return port;
                }
            }
        } catch (Exception e) {
            log.debug("无法获取客户端端口", e);
        }
        return null; // 返回null而不是0，表示端口未知
    }
}
