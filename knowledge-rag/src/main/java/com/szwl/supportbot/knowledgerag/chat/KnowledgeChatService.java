package com.szwl.supportbot.knowledgerag.chat;

import com.szwl.supportbot.knowledgerag.rag.KnowledgeRagService;
import com.szwl.supportbot.knowledgerag.session.SessionMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 知识库聊天服务
 * 整合RAG服务和会话管理，提供完整的问答体验
 */
@Slf4j
@Service
public class KnowledgeChatService {

    private final KnowledgeRagService knowledgeRagService;
    private final SessionMemory sessionMemory;

    @Autowired
    public KnowledgeChatService(KnowledgeRagService knowledgeRagService,
                               SessionMemory sessionMemory) {
        this.knowledgeRagService = knowledgeRagService;
        this.sessionMemory = sessionMemory;
    }

    /**
     * 聊天，支持知识库检索和上下文记忆
     * @param chatId 聊天ID（用户ID）
     * @param message 用户消息
     * @param businessType 业务类型
     * @return AI回答
     */
    public String chat(String chatId, String message, String businessType) {
        return chat(chatId, message, businessType, null);
    }

    /**
     * 聊天，支持知识库检索、上下文记忆和会话历史
     * @param chatId 聊天ID（用户ID）
     * @param message 用户消息
     * @param businessType 业务类型
     * @param conversationHistory 会话历史
     * @return AI回答
     */
    public String chat(String chatId, String message, String businessType, String conversationHistory) {
        try {
            log.info("开始知识库聊天: chatId={}, message={}, businessType={}, hasHistory={}", 
                     chatId, message, businessType, conversationHistory != null);

            // 直接调用知识库RAG服务，传入当前问题和历史会话
            String answer = knowledgeRagService.answerWithKnowledge(message, chatId, businessType, conversationHistory);

            log.info("知识库聊天完成: chatId={}, answerLength={}", chatId, answer.length());

            return answer;

        } catch (Exception e) {
            log.error("知识库聊天失败: chatId={}", chatId, e);
            return "抱歉，处理您的问题时出现了错误。请稍后重试或联系客服。";
        }
    }



    /**
     * 健康检查
     * @return 是否健康
     */
    public boolean isHealthy() {
        try {
            // 检查RAG服务
            boolean ragServiceHealthy = knowledgeRagService != null;
            
            // 检查会话内存
            boolean sessionMemoryHealthy = sessionMemory != null;
            
            log.debug("知识库聊天服务健康检查: ragService={}, sessionMemory={}", 
                     ragServiceHealthy, sessionMemoryHealthy);
            
            return ragServiceHealthy && sessionMemoryHealthy;
            
        } catch (Exception e) {
            log.error("知识库聊天服务健康检查失败", e);
            return false;
        }
    }
}
