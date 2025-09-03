package com.szwl.supportbot.imdemo.model;

import lombok.Data;

/**
 * 心跳消息模型
 */
@Data
public class HeartbeatMessage {
    
    private Long userId;             // 用户ID
    private Long timestamp;          // 心跳时间戳
    
    public HeartbeatMessage() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public HeartbeatMessage(Long userId) {
        this();
        this.userId = userId;
    }
}
