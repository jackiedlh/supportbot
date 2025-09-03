package com.szwl.supportbot.imdemo.model;

import lombok.Data;

/**
 * 聊天消息模型
 */
@Data
public class ChatMessage {
    
    /**
     * 消息类型
     */
    public enum MessageType {
        CHAT,           // 用户聊天消息
        SYSTEM,         // 系统消息
        AI_RESPONSE     // AI回复消息
    }
    
    private String messageId;       // 消息ID
    private MessageType type;       // 消息类型
    private String content;         // 消息内容
    private Long sender;            // 发送者ID
    private String senderName;      // 发送者名称
    private Long timestamp;         // 时间戳 (Unix时间戳，毫秒)
    private String sessionId;       // 会话ID
    
    public ChatMessage() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public ChatMessage(MessageType type, String content, Long sender) {
        this();
        this.type = type;
        this.content = content;
        this.sender = sender;
    }
}
