package com.szwl.supportbot.imdemo.service;

import com.szwl.supportbot.imdemo.model.ChatMessage;
import com.szwl.supportbot.imdemo.model.UserSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;



/**
 * 聊天服务
 * 处理用户消息，调用问题分类模块接口
 */
@Slf4j
@Service
public class ChatService {

    @Autowired
    private UserSessionService userSessionService;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${question-classifier.api.url:http://localhost:11001/api/classify}")
    private String questionClassifierApiUrl;

    /**
     * 处理用户聊天消息
     * @param message 聊天消息
     * @param sessionId 会话ID
     * @return 是否处理成功
     */
    public boolean handleUserMessage(ChatMessage message, String sessionId) {
        try {
            log.info("收到用户消息: userId={}, content={}, sessionId={}", 
                     message.getSender(), message.getContent(), sessionId);
            
            // 验证用户会话
            UserSession userSession = userSessionService.getUserSession(message.getSender());
            if (userSession == null) {
                log.warn("用户会话不存在: userId={}", message.getSender());
                return false;
            }
            
            // 使用前端传来的消息ID，不重新生成
            String messageId = message.getMessageId();
            message.setSessionId(sessionId);
            
            // 调用问题分类模块接口
            boolean success = callQuestionClassifier(message);
            
            if (success) {
                log.info("消息已发送到问题分类模块: messageId={}, userId={}", messageId, message.getSender());
                return true;
            } else {
                log.error("发送消息到问题分类模块失败: userId={}", message.getSender());
                return false;
            }
            
        } catch (Exception e) {
            log.error("处理用户消息失败: userId={}", message.getSender(), e);
            return false;
        }
    }

    /**
     * 调用问题分类模块接口
     * @param message 聊天消息
     * @return 是否调用成功
     */
    private boolean callQuestionClassifier(ChatMessage message) {
        try {
            // 构建GET请求URL，使用查询参数
            String baseUrl = questionClassifierApiUrl.replace("/api/classify", "");
            String url = String.format("%s/api/classify?question=%s&uid=%s",
                baseUrl,
                java.net.URLEncoder.encode(message.getContent(), "UTF-8"),
                message.getSender());
            
            log.debug("调用问题分类服务URL: {}", url);
            
            // 发送GET请求
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("问题分类模块调用成功: status={}, response={}", 
                         response.getStatusCode(), response.getBody());
                return true;
            } else {
                log.warn("问题分类模块调用失败: status={}, response={}", 
                         response.getStatusCode(), response.getBody());
                return false;
            }
            
        } catch (Exception e) {
            log.error("调用问题分类模块接口异常", e);
            return false;
        }
    }
}
