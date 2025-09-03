package com.szwl.supportbot.knowledgerag.rag;

import com.szwl.supportbot.knowledgerag.config.DynamicConfigService;
import com.szwl.supportbot.knowledgerag.config.RagConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;

import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 知识库 RAG 服务
 * 实现基于向量数据库的知识检索和问答功能
 * 使用QuestionAnswerAdvisor进行RAG增强
 */
@Slf4j
@Service
public class KnowledgeRagService {

    @Autowired
    private ChatModel chatModel;

    @Autowired
    private VectorStore vectorStore;

    @Autowired
    private DynamicConfigService dynamicConfigService;

    /**
     * 基于知识库回答问题
     * @param question 用户问题
     * @param sessionId 会话ID
     * @param businessType 业务类型
     * @param conversationHistory 会话历史
     * @return AI回答
     */
    public String answerWithKnowledge(String question, String sessionId, String businessType, String conversationHistory) {
        try {
            log.info("开始知识库检索问答: question={}, sessionId={}, businessType={}, hasHistory={}", 
                     question, sessionId, businessType, conversationHistory != null);

            // 从Nacos获取RAG配置
            RagConfig.RetrievalConfig retrievalConfig = dynamicConfigService.getRetrievalConfig();
            String systemPromptTemplate = dynamicConfigService.getSystemPromptTemplate();

            // 构建搜索请求
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(question)
                    .topK(retrievalConfig.getTopK())
                    .similarityThreshold(retrievalConfig.getSimilarityThreshold())
                    .build();

            log.info("构建搜索请求: topK={}, similarityThreshold={}", 
                    retrievalConfig.getTopK(), retrievalConfig.getSimilarityThreshold());

            // 构建系统提示词，包含历史会话
            String systemPrompt = buildSystemPromptWithHistory(systemPromptTemplate, conversationHistory);

            // 使用QuestionAnswerAdvisor进行RAG增强 - 参考Alibaba示例工程
            ChatClient chatClient = ChatClient.builder(chatModel)
                    .defaultSystem(systemPrompt)
                    .build();
            
            String answer = chatClient.prompt()
                    .user(question)
                    .advisors(QuestionAnswerAdvisor
                            .builder(vectorStore)
                            .searchRequest(searchRequest)
                            .build()
                    )
                    .call()
                    .content();

            log.info("知识库问答完成: question={}, answerLength={}", question, answer.length());

            return answer;

        } catch (Exception e) {
            log.error("知识库问答失败: question={}, sessionId={}", question, sessionId, e);
            return "抱歉，处理您的问题时出现了错误。请稍后重试或联系客服。";
        }
    }

    /**
     * 构建包含历史会话的系统提示词
     * @param systemPromptTemplate 系统提示词模板
     * @param conversationHistory 会话历史
     * @return 完整的系统提示词
     */
    private String buildSystemPromptWithHistory(String systemPromptTemplate, String conversationHistory) {
        // 处理历史会话内容
        String historyContent = (conversationHistory != null && !conversationHistory.trim().isEmpty()) 
            ? conversationHistory 
            : "无历史对话记录";
        
        // 构建包含历史会话的系统提示词
        String systemPrompt = systemPromptTemplate
            .replace("{conversation_history}", historyContent);
        
        return systemPrompt;
    }
}
