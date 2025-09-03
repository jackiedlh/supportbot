package com.szwl.supportbot.assistant.chat;

import com.szwl.supportbot.assistant.config.AgentConfig;
import com.szwl.supportbot.assistant.mcp.ToolProviderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * AI助手服务
 * 专注于对话逻辑，工具管理委托给ToolProviderService
 * 实现智能体功能：动态提示词、MCP工具
 */
@Slf4j
@Service
public class AssistantService {

    private final ChatModel chatModel;
    private final ToolCallbackProvider defaultTools;
    
    @Autowired
    private ToolProviderService toolProviderService;

    public AssistantService(ChatModel chatModel, ToolCallbackProvider tools) {
        this.chatModel = chatModel;
        this.defaultTools = tools;
    }

    /**
     * 根据配置动态创建ChatClient
     */
    private ChatClient createChatClientWithConfig(String chatId, String systemPrompt, AgentConfig.McpConfig mcpConfig) {
        // 创建基础的ChatClient.Builder
        ChatClient.Builder builder = ChatClient.builder(chatModel);
        
        // 设置系统提示词
        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            builder.defaultSystem(systemPrompt);
        }
        
        // 设置默认顾问
        builder.defaultAdvisors(
                new SimpleLoggerAdvisor()
        );
        
        // 通过ToolProviderService获取工具
        ToolCallbackProvider finalTools = toolProviderService.getToolsForConfig(mcpConfig);
        if (finalTools != null && finalTools.getToolCallbacks().length > 0) {
            builder.defaultToolCallbacks(finalTools.getToolCallbacks());
            log.info("成功设置MCP工具，工具数量: {}", finalTools.getToolCallbacks().length);
        } else {
            // 如果没有MCP工具，尝试使用默认工具
            if (defaultTools != null && defaultTools.getToolCallbacks().length > 0) {
                builder.defaultToolCallbacks(defaultTools.getToolCallbacks());
                log.info("使用默认工具，工具数量: {}", defaultTools.getToolCallbacks().length);
            } else {
                log.warn("没有可用的工具，ChatClient将无法使用函数调用功能");
            }
        }
        
        return builder.build();
    }

    /**
     * 聊天，支持动态配置
     */
    public String chat(String chatId, String message, String systemPrompt, AgentConfig.McpConfig mcpConfig) {
        return chat(chatId, message, systemPrompt, mcpConfig, null);
    }

    /**
     * 聊天，支持动态配置和会话历史
     */
    public String chat(String chatId, String message, String systemPrompt, AgentConfig.McpConfig mcpConfig, String conversationHistory) {
        try {
            log.info("开始对话: chatId={}, message={}, hasHistory={}", chatId, message, conversationHistory != null);
            
            // 构建完整的系统提示词
            String fullSystemPrompt = systemPrompt;
            if (conversationHistory != null && !conversationHistory.trim().isEmpty()) {
                // 替换 Nacos 配置中的占位符 {conversation_history}
                fullSystemPrompt = systemPrompt.replace("{conversation_history}", conversationHistory);
                log.info("使用历史会话上下文: chatId={}, contextLength={}", chatId, conversationHistory.length());
            } else {
                // 如果没有历史会话，替换为空字符串
                fullSystemPrompt = systemPrompt.replace("{conversation_history}", "新会话，无历史上下文");
                log.info("无历史会话上下文，使用默认值: chatId={}", chatId);
            }
            
            ChatClient client = createChatClientWithConfig(chatId, fullSystemPrompt, mcpConfig);
            
            String content = client
                    .prompt()
                    .user(message)
                    .call()
                    .content();
            
            log.info("对话完成: chatId={}, responseLength={}", chatId, content.length());
            
            return content;
            
        } catch (Exception e) {
            log.error("对话失败: chatId={}", chatId, e);
            return "抱歉，对话过程中出现了错误：" + e.getMessage();
        }
    }

    /**
     * 健康检查
     */
    public boolean isHealthy() {
        try {
            // 检查ChatModel是否可用
            boolean chatModelHealthy = chatModel != null;
            
            // 检查默认工具是否可用
            boolean toolsHealthy = defaultTools != null && defaultTools.getToolCallbacks().length > 0;
            
            // 检查工具提供者服务是否健康
            boolean toolProviderHealthy = toolProviderService != null;
            
            log.debug("健康检查: chatModel={}, tools={}, toolProvider={}", 
                     chatModelHealthy, toolsHealthy, toolProviderHealthy);
            
            return chatModelHealthy && (toolsHealthy || toolProviderHealthy);
            
        } catch (Exception e) {
            log.error("健康检查失败", e);
            return false;
        }
    }
}
