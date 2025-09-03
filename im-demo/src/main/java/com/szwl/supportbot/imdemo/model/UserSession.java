package com.szwl.supportbot.imdemo.model;

import lombok.Data;

/**
 * 用户会话模型
 * 维护用户的连接信息和路由
 */
@Data
public class UserSession {
    
    private Long userId;             // 用户ID
    private String username;         // 用户名
    private String sessionId;        // WebSocket会话ID
    private String clientIp;         // 客户端IP
    private Integer clientPort;      // 客户端端口
    private Long lastHeartbeat;      // 最近心跳时间
    private Integer missedHeartbeats; // 连续丢失心跳次数
    
    public UserSession() {
        this.lastHeartbeat = System.currentTimeMillis();
        this.missedHeartbeats = 0;
    }
    
    public UserSession(Long userId, String username, String sessionId, String clientIp, Integer clientPort) {
        this();
        this.userId = userId;
        this.username = username;
        this.sessionId = sessionId;
        this.clientIp = clientIp;
        this.clientPort = clientPort;
    }
    
    /**
     * 更新心跳时间
     */
    public void updateHeartbeat() {
        this.lastHeartbeat = System.currentTimeMillis();
        this.missedHeartbeats = 0;
    }
    
    /**
     * 增加丢失心跳次数
     */
    public void incrementMissedHeartbeats() {
        this.missedHeartbeats++;
    }
    
    /**
     * 检查是否需要清理（连续两次心跳丢失）
     */
    public boolean shouldBeCleaned() {
        return this.missedHeartbeats >= 2;
    }
}
