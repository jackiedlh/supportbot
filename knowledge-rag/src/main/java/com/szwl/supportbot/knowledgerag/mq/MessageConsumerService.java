package com.szwl.supportbot.knowledgerag.mq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.szwl.supportbot.knowledgerag.chat.KnowledgeChatService;
import com.szwl.supportbot.knowledgerag.session.SessionMemory;
import com.szwl.supportbot.knowledgerag.messaging.ImMessageService;
import com.szwl.supportbot.knowledgerag.util.MessageBodyExtractor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.SimpleConsumer;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.time.Duration;

/**
 * 消息消费服务
 * 订阅 supportbot-activity-inquiry 主题，处理活动咨询相关消息
 * 使用 RocketMQ 5.x 新API，与assistant模块保持一致
 */
@Slf4j
@Service
public class MessageConsumerService {

    private final KnowledgeChatService knowledgeChatService;
    private final SessionMemory sessionMemory;
    private final ObjectMapper objectMapper;
    private final ImMessageService imMessageService;
    
    private final String proxyServer;
    @Value("${rocketmq.consumer.topic}")
    private String topic;
    @Value("${rocketmq.consumer.group}")
    private String consumerGroup;
    
    @Value("${rocketmq.consumer.invisible-time:60}")
    private int invisibleTime;
    
    @Value("${rocketmq.consumer.await-duration:30}")
    private int awaitDuration;
    
    private SimpleConsumer simpleConsumer;

    public MessageConsumerService(
            KnowledgeChatService knowledgeChatService,
            SessionMemory sessionMemory,
            ObjectMapper objectMapper,
            ImMessageService imMessageService,
            @Value("${rocketmq.proxy-server}") String proxyServer
    ) {
        this.knowledgeChatService = knowledgeChatService;
        this.sessionMemory = sessionMemory;
        this.objectMapper = objectMapper;
        this.imMessageService = imMessageService;
        this.proxyServer = proxyServer;
    }

    /**
     * 初始化 SimpleConsumer (Spring 启动时调用)
     * 使用 SimpleConsumer 主动拉取消息
     */
    @PostConstruct
    public void init() {
        try {
            ClientServiceProvider provider = ClientServiceProvider.loadService();
            ClientConfiguration clientConfiguration = ClientConfiguration.newBuilder()
                    .setEndpoints(proxyServer)
                    .build();

            // 订阅所有消息，不进行过滤
            FilterExpression filterExpression = new FilterExpression("*", FilterExpressionType.TAG);

            this.simpleConsumer = provider.newSimpleConsumerBuilder()
                    .setClientConfiguration(clientConfiguration)
                    .setConsumerGroup(consumerGroup)
                    .setSubscriptionExpressions(Collections.singletonMap(topic, filterExpression))
                    .setAwaitDuration(Duration.ofSeconds(awaitDuration))
                    .build();

            log.info("RocketMQ SimpleConsumer 初始化成功, proxyServer={}, topic={}, group={}, invisibleTime={}s, awaitDuration={}s", 
                    proxyServer, topic, consumerGroup, invisibleTime, awaitDuration);
            
            // 启动消息消费线程
            startMessageConsumption();
        } catch (Exception e) {
            log.error("RocketMQ SimpleConsumer 初始化失败: {}", e.getMessage(), e);
        }
    }

    /**
     * Spring 关闭时释放 SimpleConsumer
     */
    @PreDestroy
    public void shutdown() {
        if (simpleConsumer != null) {
            try {
                simpleConsumer.close();
                log.info("RocketMQ SimpleConsumer 已关闭");
            } catch (Exception e) {
                log.error("关闭 RocketMQ SimpleConsumer 失败: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * 启动消息消费线程
     * 使用 SimpleConsumer 主动拉取消息
     */
    private void startMessageConsumption() {
        Thread consumptionThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 使用 SimpleConsumer 拉取消息
                    List<MessageView> messages = simpleConsumer.receive(invisibleTime, Duration.ofSeconds(awaitDuration));
                    
                    if (messages != null && !messages.isEmpty()) {
                        log.info("拉取到 {} 条消息", messages.size());
                        
                        for (MessageView messageView : messages) {
                            try {
                                log.info("开始处理消息: messageId={}", messageView.getMessageId());
                                
                                // 安全地获取消息体
                                String message = MessageBodyExtractor.extractAsString(messageView);
                                if (message == null) {
                                    log.warn("消息体提取失败，跳过处理");
                                    // 消息体提取失败，确认消息避免重复投递
                                    ackMessage(messageView, "消息体提取失败");
                                    continue;
                                }
                                
                                log.info("收到活动咨询消息: {}", message);
                                
                                // 处理消息
                                boolean success = consumeMessage(message);
                                
                                if (success) {
                                    // 消息处理成功，确认消费
                                    ackMessage(messageView, "消息处理成功");
                                } else {
                                    // 消息处理失败，确认消息避免重复投递
                                    ackMessage(messageView, "消息处理失败");
                                }
                                
                            } catch (Exception e) {
                                log.error("消息处理异常: messageId={}, error={}", 
                                        messageView.getMessageId(), e.getMessage(), e);
                                // 发生异常时，确认消息避免重复投递
                                ackMessage(messageView, "消息处理异常");
                            }
                        }
                    }
                    
                    // 短暂休眠，避免空轮询
                    Thread.sleep(100);
                    
                } catch (Exception e) {
                    log.error("消息消费线程异常: {}", e.getMessage(), e);
                    try {
                        Thread.sleep(1000); // 异常后等待1秒再重试
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, "KnowledgeRagMessageConsumptionThread");
        
        consumptionThread.setDaemon(true);
        consumptionThread.start();
        log.info("消息消费线程已启动");
    }

    /**
     * 消费消息（从消息队列）
     * 处理活动咨询相关的消息
     * 
     * @param messageJson 消息JSON字符串
     * @return true表示处理成功，false表示处理失败
     */
    public boolean consumeMessage(String messageJson) {
        try {
            log.info("收到队列消息: {}", messageJson);
            
            // 解析消息JSON
            Map<String, Object> message = parseMessage(messageJson);
            if (message == null) {
                log.error("消息解析失败，跳过处理");
                return false;
            }
            
            //提取消息内容
            String messageContent = (String) message.get("question");
            String businessType = (String) message.get("category");
            String sessionId = (String) message.get("sessionId"); // 会话ID就是用户ID
            String userId = extractUserIdFromSession(sessionId);
            
            if (messageContent == null || messageContent.trim().isEmpty()) {
                log.error("消息内容为空，跳过处理");
                return false;
            }
            
            log.info("处理消息: businessType={}, sessionId={}, message={}", 
                    businessType, sessionId, messageContent);
            
            // 如果没有指定业务类型，使用默认值
            if (businessType == null || businessType.trim().isEmpty()) {
                businessType = "default"; // 默认使用通用类型
                log.info("未指定业务类型，使用默认值: {}", businessType);
            }
            
            try {
                // 获取会话历史上下文
                String conversationHistory = sessionMemory.getConversationContext(sessionId);
                if (conversationHistory != null && !conversationHistory.trim().isEmpty()) {
                    log.info("获取会话历史上下文: sessionId={}, historyLength={}", sessionId, conversationHistory.length());
                } else {
                    log.info("会话历史上下文为空: sessionId={}", sessionId);
                }
                
                // 调用知识库聊天服务，传入会话历史
                String result = knowledgeChatService.chat(sessionId, messageContent, businessType, conversationHistory);
            
                log.info("活动咨询处理成功: businessType={}, sessionId={}, result={}", 
                        businessType, sessionId, result);
                
                // 发送AI回复给用户
                if (userId != null) {
                    try {
                        // 通过ImMessageService发送消息给用户
                        boolean sendSuccess = imMessageService.sendAiResponse(userId, result);
                        if (sendSuccess) {
                            log.info("AI回复发送成功: userId={}", userId);
                        } else {
                            log.warn("AI回复发送失败: userId={}", userId);
                        }
                    } catch (Exception e) {
                        log.error("发送AI回复异常: userId={}", userId, e);
                    }
                }
                
                // 保存用户问题和AI回答到聊天记录
                sessionMemory.addUserQuestion(sessionId, messageContent);
                sessionMemory.addAIAnswer(sessionId, result);
                
                log.info("聊天记录保存成功: sessionId={}, userQuestionLength={}, aiAnswerLength={}", 
                        sessionId, messageContent.length(), result.length());
                
                return true; // 返回成功状态
                
            } catch (Exception e) {
                log.error("活动咨询处理失败: sessionId={}", sessionId, e);
                
                // 保存用户问题和错误信息到聊天记录
                sessionMemory.addUserQuestion(sessionId, messageContent);
                sessionMemory.addAIAnswer(sessionId, "处理失败: " + e.getMessage());
                
                log.info("错误聊天记录保存成功: sessionId={}", sessionId);
                return false; // 返回失败状态
            }
            
        } catch (Exception e) {
            log.error("处理消息时发生错误", e);
            return false; // 返回失败状态
        }
    }

    /**
     * 批量消费消息
     */
    public void consumeBatchMessages(java.util.List<String> messages) {
        try {
            log.info("批量消费消息: count={}", messages.size());
            for (String message : messages) {
                consumeMessage(message);
            }
        } catch (Exception e) {
            log.error("批量消费消息失败", e);
        }
    }

    /**
     * 解析消息JSON
     */
    private Map<String, Object> parseMessage(String messageJson) {
        try {
            log.info("开始解析消息JSON，长度: {}", messageJson != null ? messageJson.length() : 0);
            Map<String, Object> result = objectMapper.readValue(messageJson, Map.class);
            log.info("消息JSON解析成功，解析结果: {}", result);
            return result;
        } catch (Exception e) {
            log.error("消息JSON解析失败: messageJson={}, error={}", messageJson, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从会话ID中提取用户ID
     * 现在会话ID直接就是用户ID，所以直接返回
     */
    private String extractUserIdFromSession(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return null;
        }
        
        // 会话ID直接就是用户ID
        return sessionId;
    }

    /**
     * 安全地确认消息，避免重复确认和异常
     * 
     * @param messageView 消息视图
     * @param reason 确认原因
     */
    private void ackMessage(MessageView messageView, String reason) {
        try {
            simpleConsumer.ack(messageView);
            log.info("消息确认成功: messageId={}, reason={}", messageView.getMessageId(), reason);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("INVALID_RECEIPT_HANDLE")) {
                log.warn("消息确认失败，receipt handle已过期: messageId={}, reason={}, error={}", 
                        messageView.getMessageId(), reason, e.getMessage());
            } else {
                log.error("消息确认失败: messageId={}, reason={}, error={}", 
                        messageView.getMessageId(), reason, e.getMessage(), e);
            }
        }
    }



    /**
     * 健康检查
     */
    public boolean isHealthy() {
        try {
            // 检查其他依赖服务
            boolean servicesHealthy = knowledgeChatService != null && 
                                   sessionMemory != null &&
                                   simpleConsumer != null;
            
            log.debug("健康检查: services={}", servicesHealthy);
            
            return servicesHealthy;
            
        } catch (Exception e) {
            log.error("健康检查失败", e);
            return false;
        }
    }
}
