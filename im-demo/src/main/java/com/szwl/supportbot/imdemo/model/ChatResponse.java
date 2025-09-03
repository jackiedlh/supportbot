package com.szwl.supportbot.imdemo.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 聊天消息响应模型
 * 用于返回消息处理结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatResponse {
    
    private String type = "CHAT_RESPONSE";  // 消息类型，用于前端识别
    private int code;           // 状态码：0表示成功，-1表示失败
    private String message;     // 响应消息
    private String messageId;   // 原始消息ID，用于前端匹配
    
    /**
     * 创建成功响应
     */
    public static ChatResponse success(String messageId) {
        return new ChatResponse("CHAT_RESPONSE", 0, "消息处理成功", messageId);
    }
    
    /**
     * 创建成功响应（带自定义消息）
     */
    public static ChatResponse success(String messageId, String message) {
        return new ChatResponse("CHAT_RESPONSE", 0, message, messageId);
    }
    
    /**
     * 创建失败响应
     */
    public static ChatResponse failure(String messageId) {
        return new ChatResponse("CHAT_RESPONSE", -1, "消息处理失败", messageId);
    }
    
    /**
     * 创建失败响应（带自定义消息）
     */
    public static ChatResponse failure(String messageId, String message) {
        return new ChatResponse("CHAT_RESPONSE", -1, message, messageId);
    }
}
