package com.szwl.supportbot.imdemo.websocket;

import com.szwl.supportbot.imdemo.model.HeartbeatMessage;
import com.szwl.supportbot.imdemo.service.UserSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

/**
 * 心跳消息处理器
 * 处理用户心跳消息
 */
@Slf4j
@Controller
public class HeartbeatHandler {

    @Autowired
    private UserSessionService userSessionService;

    /**
     * 处理用户心跳消息
     */
    @MessageMapping("/heartbeat")
    public void handleHeartbeat(@Payload HeartbeatMessage heartbeat) {
        log.debug("收到心跳消息: userId={}, timestamp={}", 
                  heartbeat.getUserId(), heartbeat.getTimestamp());
        
        // 处理心跳
        boolean success = userSessionService.handleHeartbeat(heartbeat.getUserId());
        
        if (!success) {
            log.warn("心跳处理失败: userId={}", heartbeat.getUserId());
        }
    }
}
