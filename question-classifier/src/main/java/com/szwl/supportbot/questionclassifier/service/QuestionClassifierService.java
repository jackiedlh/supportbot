package com.szwl.supportbot.questionclassifier.service;

import com.szwl.supportbot.questionclassifier.config.PromptTemplateConfig;
import com.szwl.supportbot.questionclassifier.entity.ClassificationResult;
import com.szwl.supportbot.questionclassifier.service.SessionMemory;
import com.szwl.supportbot.questionclassifier.entity.QuestionClassificationResult;
import com.szwl.supportbot.questionclassifier.enums.ClassificationErrorCode;
import com.szwl.supportbot.questionclassifier.mq.TaskProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

@Slf4j
@Service
public class QuestionClassifierService {

    private final ChatClient chatClient;
    private final SessionMemory sessionMemory;
    private final TaskProducer taskProducer;
    private final PromptTemplateConfig promptTemplateConfig;

    private final PromptBuilder promptBuilder;
    private final ResultParser resultParser;
    
    @Value("${ai.classification.max-retries:3}")
    private int maxRetries;
    
    @Value("${ai.classification.timeout:30000}")
    private int timeout;

    public QuestionClassifierService(ChatClient.Builder builder, 
                                   SessionMemory sessionMemory, 
                                   TaskProducer taskProducer,
                                   PromptTemplateConfig promptTemplateConfig) {
        this.chatClient = builder.build();
        this.sessionMemory = sessionMemory;
        this.taskProducer = taskProducer;
        this.promptTemplateConfig = promptTemplateConfig;
        this.promptBuilder = new PromptBuilder(promptTemplateConfig);
        this.resultParser = new ResultParser();
    }


    /**
     * 问题分类接口：等待AI分析结果
     * @return 包含错误码、原因和数据的分类结果
     */
    public ClassificationResult classifyQuestion(String question, String sessionId) {
        try {
            // 获取对话历史上下文作为分类模型的记忆
            String context = sessionMemory.getConversationContext(sessionId);
            if (context == null || context.trim().isEmpty()) {
                context = "新会话，无历史上下文";
                log.info("会话上下文为空，使用默认上下文: sessionId={}", sessionId);
            } else {
                log.info("获取到会话历史上下文: sessionId={}, contextLength={}", sessionId, context.length());
            }
            
            // 构建提示词
            String prompt = promptBuilder.buildClassificationPrompt(question, context);
            if (prompt == null || prompt.trim().isEmpty()) {
                log.error("提示词构建失败");
                return ClassificationResult.failure(ClassificationErrorCode.CONFIG_ERROR);
            }
            
            // 调用AI模型
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    QuestionClassificationResult classificationResult = callAIModel(prompt, context);
                    
                    if (classificationResult != null && classificationResult.getQuestions() != null && !classificationResult.getQuestions().isEmpty()) {
                        // 处理分类结果
                        processClassificationResult(classificationResult, question, sessionId);
                        log.info("问题分类成功，共处理 {} 个问题", classificationResult.getQuestions().size());
                        return ClassificationResult.success();
                    } else {
                        log.warn("AI返回结果为空或格式无效，第{}次尝试", attempt);
                        if (attempt == maxRetries) {
                            return ClassificationResult.failure(ClassificationErrorCode.AI_RESULT_EMPTY);
                        }
                    }
                    
                } catch (Exception e) {
                    if (attempt == maxRetries) {
                        log.error("问题分类失败，已重试{}次: {}", maxRetries, e.getMessage());
                        return ClassificationResult.failure(ClassificationErrorCode.AI_CALL_FAILED);
                    }
                    log.warn("问题分类失败，第{}次尝试: {}", attempt, e.getMessage());
                }
            }
            
            return ClassificationResult.failure(ClassificationErrorCode.AI_CALL_FAILED);
            
        } catch (Exception e) {
            log.error("问题分类过程中发生系统错误: {}", e.getMessage(), e);
            return ClassificationResult.failure(ClassificationErrorCode.SYSTEM_ERROR);
        }
    }

    /**
     * 调用AI模型，直接返回结构化结果
     */
    private QuestionClassificationResult callAIModel(String prompt, String context) {
        try {
            log.info("开始调用AI模型进行分类");
            log.debug("提示词长度: {}", prompt.length());
            log.debug("上下文长度: {}", context != null ? context.length() : 0);
            
            // 获取AI响应字符串
            String aiResponse = chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();
            
            // 使用ResultParser解析为结构化对象
            QuestionClassificationResult result = resultParser.parseResult(aiResponse);
            
            log.info("AI模型调用成功，返回结构化结果");
            return result;
            
        } catch (Exception e) {
            log.error("AI模型调用失败: {}", e.getMessage(), e);
            throw new RuntimeException("AI模型调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 处理分类结果
     */
    private void processClassificationResult(QuestionClassificationResult result, String originalQuestion, String sessionId) {
        log.info("处理分类结果，共 {} 个问题", result.getQuestions().size());
        
        // 统一循环处理所有问题
        for (QuestionClassificationResult.QuestionItem questionItem : result.getQuestions()) {
            taskProducer.sendTaskToQueue(questionItem, questionItem.getOriginalText(), sessionId);
        }
    }

}
