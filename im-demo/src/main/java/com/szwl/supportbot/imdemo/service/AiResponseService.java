package com.szwl.supportbot.imdemo.service;

import com.szwl.supportbot.imdemo.model.AiResponseRequest;
import com.szwl.supportbot.imdemo.model.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * AI回复服务
 * 处理AI模块的回复并发送给用户
 */
@Slf4j
@Service
public class AiResponseService {

    @Autowired
    private UserSessionService userSessionService;

    /**
     * 发送AI回复给用户
     * @param request AI回复请求
     * @return 是否发送成功
     */
    public boolean sendAiResponse(AiResponseRequest request) {
        try {
            log.info("处理AI回复: userId={}, source={}", request.getUserId(), request.getSource());
            
            // 创建AI回复消息
            ChatMessage aiMessage = new ChatMessage(
                ChatMessage.MessageType.AI_RESPONSE,
                request.getContent(),
                0L  // AI系统用户ID
            );
            aiMessage.setMessageId(UUID.randomUUID().toString());
            aiMessage.setSenderName("AI助手");
            
            // 发送给用户
            boolean success = userSessionService.sendMessageToUser(request.getUserId(), aiMessage);
            
            if (success) {
                log.info("AI回复发送成功: userId={}, content={}", request.getUserId(), request.getContent());
            } else {
                log.warn("AI回复发送失败: userId={}", request.getUserId());
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("发送AI回复异常: userId={}", request.getUserId(), e);
            return false;
        }
    }
}
