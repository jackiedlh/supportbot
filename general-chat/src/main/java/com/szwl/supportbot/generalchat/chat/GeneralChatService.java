package com.szwl.supportbot.generalchat.chat;

import com.szwl.supportbot.generalchat.config.DynamicConfigService;
import com.szwl.supportbot.generalchat.session.SessionMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 通用聊天服务
 * 直接使用AI模型问答，维护上下文记忆
 */
@Slf4j
@Service
public class GeneralChatService {

    private final ChatModel chatModel;
    private final SessionMemory sessionMemory;
    private final DynamicConfigService dynamicConfigService;

    @Autowired
    public GeneralChatService(ChatModel chatModel, 
                             SessionMemory sessionMemory,
                             DynamicConfigService dynamicConfigService) {
        this.chatModel = chatModel;
        this.sessionMemory = sessionMemory;
        this.dynamicConfigService = dynamicConfigService;
    }

    /**
     * 聊天，支持上下文记忆和会话历史
     * @param chatId 聊天ID（用户ID）
     * @param message 用户消息
     * @param conversationHistory 会话历史
     * @return AI回答
     */
    public String chat(String chatId, String message, String conversationHistory) {
        try {
            log.info("开始通用聊天: chatId={}, message={}, hasHistory={}", 
                     chatId, message, conversationHistory != null);

            // 从Nacos获取系统提示词模板
            String systemPromptTemplate = dynamicConfigService.getSystemPromptTemplate();
            if (systemPromptTemplate == null || systemPromptTemplate.trim().isEmpty()) {
                log.error("系统提示词模板未配置，无法生成回答");
                return "抱歉，系统配置不完整，无法处理您的问题。请联系管理员。";
            }

            // 构建完整的系统提示词
            String fullSystemPrompt = systemPromptTemplate;
            if (conversationHistory != null && !conversationHistory.trim().isEmpty()) {
                // 替换 Nacos 配置中的占位符 {conversation_history}
                fullSystemPrompt = systemPromptTemplate.replace("{conversation_history}", conversationHistory);
                log.info("使用历史会话上下文: chatId={}, contextLength={}", chatId, conversationHistory.length());
            } else {
                // 如果没有历史会话，替换为空字符串
                fullSystemPrompt = systemPromptTemplate.replace("{conversation_history}", "新会话，无历史上下文");
                log.info("无历史会话上下文，使用默认值: chatId={}", chatId);
            }

            // 创建ChatClient
            ChatClient client = ChatClient.builder(chatModel)
                    .defaultSystem(fullSystemPrompt)
                    .defaultAdvisors(new SimpleLoggerAdvisor())
                    .build();

            // 生成回答
            String answer = client.prompt()
                    .user(message)
                    .call()
                    .content();

            log.info("通用聊天完成: chatId={}, answerLength={}", chatId, answer.length());

            return answer;

        } catch (Exception e) {
            log.error("通用聊天失败: chatId={}", chatId, e);
            return "抱歉，处理您的问题时出现了错误。请稍后重试或联系客服。";
        }
    }



    /**
     * 健康检查
     * @return 是否健康
     */
    public boolean isHealthy() {
        try {
            // 检查聊天模型
            boolean chatModelHealthy = chatModel != null;
            
            // 检查会话内存
            boolean sessionMemoryHealthy = sessionMemory != null;
            
            // 检查配置
            boolean configHealthy = dynamicConfigService != null && 
                                  dynamicConfigService.getSystemPromptTemplate() != null && 
                                  !dynamicConfigService.getSystemPromptTemplate().trim().isEmpty();
            
            log.debug("通用聊天服务健康检查: chatModel={}, sessionMemory={}, config={}", 
                     chatModelHealthy, sessionMemoryHealthy, configHealthy);
            
            return chatModelHealthy && sessionMemoryHealthy && configHealthy;
            
        } catch (Exception e) {
            log.error("通用聊天服务健康检查失败", e);
            return false;
        }
    }
}
